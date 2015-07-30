/*
 * Copyright 2012-2014 Nikolay A. Viguro
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.iris.common.voice;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;

public class YandexSynthesiser implements Synthesiser {

    private static final String YANDEX_SYNTHESISER_URL = "https://tts.voicetech.yandex.net/generate";

    /**
     * API_KEY used for requests
     */
    private final String API_KEY;

    /**
     * Speaker voice (jane, omazh, zahar, ermil)
     */
    private String speaker = "ermil";

    /**
     * Constructor
     *
     * @param API_KEY The Yandex API-Key
     */
    public YandexSynthesiser(String API_KEY) {
        this.API_KEY = API_KEY;
    }

    /**
     * Get speaker voice
     */
    public String getSpeaker() {
        return speaker;
    }

    /**
     * Set speaker voice
     *
     * @param speaker voice - jane, omazh, zahar or ermil
     */
    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    @Override
    public void setLanguage(String language) {

    }

    /**
     * Gets an input stream to MP3 data for the returned information from a request
     *
     * @param synthText Text you want to be synthesized into MP3 data
     * @return Returns an input stream of the MP3 data that is returned from Google
     * @throws java.io.IOException Throws exception if it can not complete the request
     */
    public InputStream getMP3Data(String synthText) throws IOException {

        String languageCode = "ru-RU";

        if (synthText.length() > 100) {
            List<String> fragments = parseString(synthText);//parses String if too long
            InputStream out = getMP3Data(fragments);
            return out;
        }

        String encoded = URLEncoder.encode(synthText, "UTF-8"); //Encode

        StringBuilder sb = new StringBuilder(YANDEX_SYNTHESISER_URL);
        sb.append("?key=" + API_KEY);
        sb.append("&text=" + encoded);
        sb.append("&lang=" + languageCode);
        sb.append("&format=mp3");
        sb.append("&speaker=" + speaker);

        URL url = new URL(sb.toString()); //create url

        // Open New URL connection channel.
        URLConnection urlConn = url.openConnection(); //Open connection

        urlConn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:2.0) Gecko/20100101 Firefox/4.0"); //Adding header for user agent is required

        return urlConn.getInputStream();
    }

    /**
     * Gets an InputStream to MP3Data for the returned information from a request
     *
     * @param synthText List of Strings you want to be synthesized into MP3 data
     * @return Returns an input stream of all the MP3 data that is returned from Google
     * @throws java.io.IOException Throws exception if it cannot complete the request
     */
    public InputStream getMP3Data(List<String> synthText) throws IOException {
        //Uses an executor service pool for concurrency. Limit to 1000 threads max.
        ExecutorService pool = Executors.newFixedThreadPool(1000);
        //Stores the Future (Data that will be returned in the future)
        Set<Future<InputStream>> set = new LinkedHashSet<Future<InputStream>>(synthText.size());
        for (String part : synthText) { //Iterates through the list
            Callable<InputStream> callable = new MP3DataFetcher(part);//Creates Callable
            Future<InputStream> future = pool.submit(callable);//Begins to run Callable
            set.add(future);//Adds the response that will be returned to a set.
        }
        List<InputStream> inputStreams = new ArrayList<InputStream>(set.size());
        for (Future<InputStream> future : set) {
            try {
                inputStreams.add(future.get());//Gets the returned data from the future.
            } catch (ExecutionException e) {//Thrown if the MP3DataFetcher encountered an error.
                Throwable ex = e.getCause();
                if (ex instanceof IOException) {
                    throw (IOException) ex;//Downcasts and rethrows it.
                }
            } catch (InterruptedException e) {//Will probably never be called, but just in case...
                Thread.currentThread().interrupt();//Interrupts the thread since something went wrong.
            }
        }
        return new SequenceInputStream(Collections.enumeration(inputStreams));//Sequences the stream.
    }

    /**
     * Separates a string into smaller parts so that Google will not reject the request.
     *
     * @param input The string you want to separate
     * @return A List<String> of the String fragments from your input..
     */
    private List<String> parseString(String input) {
        return parseString(input, new ArrayList<String>());
    }

    /**
     * Separates a string into smaller parts so that Google will not reject the request.
     *
     * @param input     The string you want to break up into smaller parts
     * @param fragments List<String> that you want to add stuff too.
     *                  If you don't have a List<String> already constructed "new ArrayList<String>()" works well.
     * @return A list of the fragments of the original String
     */
    private List<String> parseString(String input, List<String> fragments) {
        if (input.length() <= 100) {//Base Case
            fragments.add(input);
            return fragments;
        } else {
            int lastWord = findLastWord(input);//Checks if a space exists
            if (lastWord <= 0) {
                fragments.add(input.substring(0, 100));//In case you sent gibberish to Google.
                return parseString(input.substring(100), fragments);
            } else {
                fragments.add(input.substring(0, lastWord));//Otherwise, adds the last word to the list for recursion.
                return parseString(input.substring(lastWord), fragments);
            }
        }
    }

    /**
     * Finds the last word in your String (before the index of 99) by searching for spaces and ending punctuation.
     * Will preferably parse on punctuation to alleviate mid-sentence pausing
     *
     * @param input The String you want to search through.
     * @return The index of where the last word of the string ends before the index of 99.
     */
    private int findLastWord(String input) {
        if (input.length() < 100)
            return input.length();
        int space = -1;
        for (int i = 99; i > 0; i--) {
            char tmp = input.charAt(i);
            if (isEndingPunctuation(tmp)) {
                return i + 1;
            }
            if (space == -1 && tmp == ' ') {
                space = i;
            }
        }
        if (space > 0) {
            return space;
        }
        return -1;
    }

    /**
     * Checks if char is an ending character
     * Ending punctuation for all languages according to Wikipedia (Except for Sanskrit non-unicode)
     *
     * @param input char you want check
     * @return True if it is, false if not.
     */
    private boolean isEndingPunctuation(char input) {
        return input == '.' || input == '!' || input == '?' || input == ';' || input == ':' || input == '|';
    }

    /**
     * This class is a callable.
     * A callable is like a runnable except that it can return data and throw exceptions.
     * Useful when using futures. Dramatically improves the speed of execution.
     *
     * @author Aaron Gokaslan (Skylion)
     */
    private class MP3DataFetcher implements Callable<InputStream> {
        private String synthText;

        public MP3DataFetcher(String synthText) {
            this.synthText = synthText;
        }

        public InputStream call() throws IOException {
            return getMP3Data(synthText);
        }
    }

}