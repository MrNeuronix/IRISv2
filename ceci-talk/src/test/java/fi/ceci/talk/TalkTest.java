package fi.ceci.talk;

import com.darkprograms.speech.microphone.MicrophoneAnalyzer;
import com.darkprograms.speech.recognizer.GoogleResponse;
import com.darkprograms.speech.recognizer.Recognizer;
import com.darkprograms.speech.synthesiser.Synthesiser;
import javaFlacEncoder.FLACFileWriter;
import javazoom.jl.player.Player;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import javax.sound.sampled.AudioFileFormat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Class for testing talk functionality
 */
public class TalkTest {

    @Test
    @Ignore
    public void testTalk() throws Exception {

        Synthesiser synthesiser = new Synthesiser(Recognizer.Languages.FINNISH.toString());
        Recognizer rec = new Recognizer(Recognizer.Languages.FINNISH);


        final String sanoFile = "target/sano";
        if (!new File(sanoFile).exists()) {
            IOUtils.copy(synthesiser.getMP3Data("Sano"), new FileOutputStream(sanoFile, false));
        }


        boolean shutdown = false;
        String filename = "target/testfile";
        while (!shutdown) {
            filename += "X";
            final Player sanoPlayer = new Player(new FileInputStream(sanoFile));
            final MicrophoneAnalyzer mic = new MicrophoneAnalyzer(AudioFileFormat.Type.WAVE);
            mic.open();


            final int startThreshold = 40;
            final int stopThreshold = 30;
            int avgVolume = 0;
            boolean speaking = false;
            avgVolume = mic.getAudioVolume();
            long captureStartMillis = System.currentTimeMillis();
            for(int i = 0; i<1000||speaking; i++){
                int volume = mic.getAudioVolume();
                avgVolume = (2 * avgVolume + 1 * volume) / 3;
                System.out.println(volume + " " + avgVolume);
                if(! speaking && avgVolume > startThreshold) {
                    System.out.println("Playing sano.");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                sanoPlayer.play();
                            } catch (final Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    Thread.sleep(500);
                    System.out.println("Done playing.");
                    mic.captureAudioToFile(filename);
                    speaking = true;
                    System.out.println("speaking");
                    captureStartMillis = System.currentTimeMillis();
                }
                if(System.currentTimeMillis() - captureStartMillis > 1000 && speaking && stopThreshold > avgVolume){
                    System.out.println("done speaking");
                    break;
                }
                /*if (!speaking && System.currentTimeMillis() - lastCaptureStartMills > 5000) {
                    System.out.println("recapturing");
                }*/
                Thread.sleep(100);
            }
            mic.close();

            GoogleResponse response = rec.getRecognizedDataForWave(filename);
            String out = response.getResponse();
            String confidence = response.getConfidence();
            System.out.println(confidence);
            System.out.println(out);

            if (out != null && Float.parseFloat(confidence) > 0.2) {
                final Player player = new Player(synthesiser.getMP3Data(out));
                player.play();
                player.close();
                if (out.contains("lopeta")) {
                    shutdown = true;
                }
            }

            sanoPlayer.close();
            new File(filename).deleteOnExit();

        }

    }

}
