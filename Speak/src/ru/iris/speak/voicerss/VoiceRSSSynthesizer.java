package ru.iris.speak.voicerss;

/**
 * Created with IntelliJ IDEA.
 * Author: Nikolay A. Viguro
 * Date: 09.09.12
 * Time: 13:07
 * License: GPL v3
 * Based on code of Samir Ahmed
 */

import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import javazoom.jl.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.httpGET;
import ru.iris.speak.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SYNTHESIZER MODULE --
 * LAST EDITED BY SAMIR AHMED - JUNE 20 2011
 * <p/>
 * OUTLINE: Synthesizer Used for Generating Speech Via Google Translate Text-to-Speech API
 * This Class uses a CachedThreadPool To Increase Increase Efficiency
 * Speed Say: Single Thread 4.3 seconds vs Multi-Threaded 1.2 Seconds
 */

public class VoiceRSSSynthesizer implements Runnable
{

    /* Synthesizer Data Fields
      *
      * UserText		: LinkedList: String Container with Tokenized Stringfs
      * Files		: Integer 	: The number of output files that will be generated
      * isUnassigned : Boolean	: True/False holds status of the AudioDevice object
      *
      * AudioLine and FutureAudioLine are AudioDevice objects for the JLayer Player
      *
      **/

    private LinkedList<String> UserText;
    private Integer Files;
    private boolean IsUnassigned;
    private AudioDevice AudioLine;

    private final ExecutorService exs;
    private final String SPEAK_URL = "http://api.voicerss.org/?key=" + Service.config.get("voicerssAPI") + "&hl=ru-ru&f=48khz_16bit_stereo&c=mp3&src=";
    private final Future<AudioDevice> futureAudioLine;
    private static Logger log = LoggerFactory.getLogger (VoiceRSSSynthesizer.class.getName ());

    /** Constructor: Will submit the AudioDeviceLoader to the executor */

    /**
     * Constructor for Synthesizer Object*
     */
    public VoiceRSSSynthesizer(ExecutorService executor)
    {
        //Assign executor
        this.exs = executor;
        this.IsUnassigned = true;
        AudioDeviceLoader ADL = new AudioDeviceLoader ();
        futureAudioLine = this.exs.submit (ADL);
    }

    /**
     * Assigns the audioline if unassigned
     */

    private void AssignAudioLine()
    {

        // If unassigned we assign it and flag as assigned, otherwise ignore
        if(this.IsUnassigned)
        {
            try
            {
                AudioLine = futureAudioLine.get ();
                this.IsUnassigned = false;
            } catch (Exception ee)
            {
                ee.printStackTrace ();
            }
        }
    }

    /**
     * Public method to set desire text for Text to Speech Synthesis
     */

    public void setAnswer(String rawText)
    {

        // Assign the audiodevice from the future it is stored in
        AssignAudioLine ();

        //Convert to good ole' plain english ascii characters
        rawText = Normalizer.normalize (rawText, Normalizer.Form.NFD)
                .replaceAll ("\\p{InCombiningDiacriticalMarks}+", "");

        // Ensure we have only whitespaces that are single spaces characters
        rawText = rawText.replaceAll ("%", " процент ");
        rawText = rawText.replaceAll ("&", " и ");
        rawText = rawText.replaceAll ("\\+", " плюс ");

        rawText = rawText.replaceAll ("\\s", " ");
        rawText = rawText.replaceAll ("\\s{2,}", " ");
        rawText = rawText.replaceAll ("\"|\'|\\(|\\)|\\]|\\[", "");

        // Tokenize userText and Retreive number of Files;
        UserText = smartTokenize (rawText);
        Files = UserText.size ();
    }

    /**
     * SmartTokenize : Function for parsing by grammar and restructuring output: Average Run Time ~ 12 ms *
     */
    private LinkedList<String> smartTokenize(String oneString)
    {
        // Check if we end on a period. If not Add one
        if(!oneString.endsWith ("."))
        {
            oneString = oneString + ".";
        }

        // Create a regex pattern parse by punctuation;
        Pattern tokenizerPattern = Pattern.compile ("([^\\.,;:!]*[\\.,;:!])+?");
        Matcher tokenizerMatcher = tokenizerPattern.matcher (oneString);

        // Declare String variables.
        String buffer;
        String sentence = "";
        int count = 0;
        int length = 0;

        // Create Linked list for putting in strings
        LinkedList<String> answerList = new LinkedList<String> ();

        // For every punctuation separated value
        while (tokenizerMatcher.find ())
        {

            // Instantiate Buffer count and length;
            buffer = tokenizerMatcher.group ();
            count = sentence.length ();
            length = buffer.length ();

            // CASE 1: We can easily add buffer to our sentence because we are under the 100 char limit
            if((count + length) <= 100)
            {
                sentence += buffer;
            }

            // CASE 2: If buffer length is greater than 100...
            else if(length > 100)
            {

                // Add tack on however much of buffer is required to get a 100 character string
                int breakpoint = buffer.lastIndexOf (' ', 100 - sentence.length ());
                answerList.addLast (sentence + buffer.subSequence (0, breakpoint));
                length -= breakpoint;

                // If we still have more than 100 characters we cut off the largest chunks until we are under one hundred
                if(length > 100)
                {
                    while (length > 100)
                    {
                        int newbreakpoint = buffer.lastIndexOf (' ', breakpoint + 100);
                        answerList.addLast (buffer.substring (breakpoint, newbreakpoint));
                        length -= (newbreakpoint - breakpoint);
                        breakpoint = newbreakpoint;
                    }
                }

                // In either case, we just tack on the remainder, assuming it is not zero
                if(length > 0)
                {
                    answerList.addLast (buffer.substring (breakpoint, buffer.length ()));
                    sentence = "";
                }
            }

            // CASE 3: Buffer is less than 100 but added to sentence is greater
            // We add sentence too list, we remake sentence as buffer
            else
            {
                answerList.addLast (sentence);
                sentence = buffer;
            }
        }
        // Last case in the event we missed something
        if(sentence != "")
        {
            answerList.addLast (sentence);
        }

        return answerList;
    }

    /** getSpeech **/

    /**
     * Speak  will create multiple SpeechFileGenerator Objects, which return mp3 chunks
     * the method will concatenate all the files and then play them with the JLayer Mp3 Player Object.
     */
    private synchronized void Speak(LinkedList<String> UserText, Integer Files)
    {

        // Use the executor service to submit all the TTS strings simultaneously
        try
        {
            // For every file in UserText LinkedList; Generate a textToSpeech Object and execute it
            ArrayList<Future<byte[]>> mp3_filelist = new ArrayList<Future<byte[]>> (Files);
            for (int ii = 1; ii <= Files; ii++)
            {
                SpeechFileGenerator tts = new SpeechFileGenerator (UserText.pop ());
                mp3_filelist.add (exs.submit (tts));
            }

            /* Create a new mp3 file byte[] of size zero*/
            byte[] mp3_file = new byte[0];
            int size = mp3_file.length;

            /* For every future in the arrayList,
                * Append it to mp3_file variable;
                * */
            for (Future<byte[]> mp3_piece_Future : mp3_filelist)
            {

                /* Load the future into mp3_piece */
                byte[] mp3_piece = mp3_piece_Future.get ();

                /* Calculate piece length */
                int pieceLength = mp3_piece.length;

                /* Create a new temporary byte array with size large enough to accomadate the new piece  */
                byte[] temp = new byte[size + pieceLength];

                /* Copy the original file into the byte array */
                System.arraycopy (mp3_file, 0, temp, 0, size);

                /* Copy the new piece into the byte array, offset by the old files length */
                System.arraycopy (mp3_piece, 0, temp, size, pieceLength);

                /* Update the size of the whole file and reassign variables */
                mp3_file = temp;
                size += pieceLength;

            }

            /* Create Input Stream from mp3_file and submit to JLayer MP3 Player*/
            InputStream mp3_stream = new ByteArrayInputStream (mp3_file);
            Player mp3_player = new Player (mp3_stream, this.AudioLine);

            /* Print Response time and play*/
            mp3_player.play ();
        } catch (Exception e)
        {
            e.printStackTrace ();
        }
    }

    /**
     * Run Interface*
     */
    public void run()
    {
        try
        {
            Speak (UserText, Files);
        } catch (Exception ee)
        {
            ee.printStackTrace ();
        }
    }

    /**
     * Callable Object to loads audioDevice from registry for JLayer MP3 Player
     * Tends to take 3 seconds for the the AudioDevice to Load
     */
    private class AudioDeviceLoader implements Callable<AudioDevice>
    {

        /**
         * Implements callable interface: Loads the AudioDevice and returns it
         */
        public AudioDevice call()
        {
            try
            {
                // Get AudioDevice from Registry
                FactoryRegistry factoryregistry = FactoryRegistry.systemRegistry ();
                AudioDevice audio = factoryregistry.createAudioDevice ();
                return audio;
            } catch (Exception ee)
            {
                System.err.println ("[voice] Could not load audio driver");
                ee.printStackTrace ();
                return null;
            }
        }

    }

    /**
     * Threadable textToSpeech Synthesis Objects *
     */
    private class SpeechFileGenerator implements Callable<byte[]>
    {

        /* textToSpeech Data Fields
           * text		: String 	: String to pass to the ttsShell
           * mp3_file	: byte[]	: Byte Array with mp3 file contained in it
           * */

        private String text;
        private byte[] mp3_chunk;

        /**
         * Constructor - Takes String Under 100 Characters and Encodes to URL safe Format*
         */
        public SpeechFileGenerator(String text)
        {
            try
            {
                this.text = URLEncoder.encode (text, "UTF-8");
            } catch (UnsupportedEncodingException UEex)
            {
                // If the result is unencodable by the means above,
                // we replace the spaces with plus and hope for the best
                this.text = text.replaceAll (" ", "+");
            }
        }

        /**
         * "call" : Callable method, will return future byte [] with the mp3 file containing the give text *
         */
        public byte[] call()
        {
            try
            {
                //
                mp3_chunk = httpGET.downloadToByteArray(SPEAK_URL + text, this);
            } catch (Exception ee)
            {
                ee.printStackTrace ();
                mp3_chunk = new byte[0];
            }

            return mp3_chunk;
        }
    }
}
