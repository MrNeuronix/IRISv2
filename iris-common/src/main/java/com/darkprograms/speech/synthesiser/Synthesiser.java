package com.darkprograms.speech.synthesiser;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.darkprograms.speech.translator.GoogleTranslate;



/*******************************************************************************
 * Synthesiser class that connects to Google's unoffical API to retrieve data
 *
 * @author Luke Kuza, Aaron Gokaslan (Skylion)
 *******************************************************************************/
public class Synthesiser {

	/**
	 * URL to query for Google synthesiser
	 */
	private final static String GOOGLE_SYNTHESISER_URL = "http://translate.google.com/translate_tts?tl=";

	/**
	 * language of the Text you want to translate
	 */
	private String languageCode; 

	/**
	 * LANG_XX_XXXX Variables are language codes. 
	 */
	public static final String LANG_AU_ENGLISH = "en-AU";
	public static final String LANG_US_ENGLISH = "en-US";
	public static final String LANG_UK_ENGLISH = "en-GB";
	public static final String LANG_ES_SPANISH = "es";
	public static final String LANG_FR_FRENCH = "fr";
	public static final String LANG_DE_GERMAN = "de";
	public static final String LANG_PT_PORTUGUESE = "pt-pt";
	public static final String LANG_PT_BRAZILIAN = "pt-br";
	//Please add on more regional languages as you find them. Also try to include the accent code if you can can.

	/**
	 * Constructor
	 */
	public Synthesiser() {
		languageCode = "auto";
	}

	/**
	 * Constructor that takes language code parameter. Specify to "auto" for language autoDetection 
	 */
	public Synthesiser(String languageCode){
		this.languageCode = languageCode;
	}

	/**
	 * Returns the current language code for the Synthesiser.
	 * Example: English(Generic) = en, English (US) = en-US, English (UK) = en-GB. and Spanish = es;
	 * @return the current language code parameter
	 */
	public String getLanguage(){
		return languageCode;
	}

	/**
	 * Note: set language to auto to enable automatic language detection.
	 * Setting to null will also implement Google's automatic language detection
	 * @param languageCode The language code you would like to modify languageCode to.
	 */
	public void setLanguage(String languageCode){
		this.languageCode = languageCode;
	}

	/**
	 * Gets an input stream to MP3 data for the returned information from a request
	 *
	 * @param synthText Text you want to be synthesized into MP3 data
	 * @return Returns an input stream of the MP3 data that is returned from Google
	 * @throws Exception Throws exception if it can not complete the request
	 */
	public InputStream getMP3Data(String synthText) throws Exception {

		String languageCode = this.languageCode;//Ensures retention of language settings if set to auto

		if(languageCode == null || languageCode.equals("") || languageCode.equalsIgnoreCase("auto")){
			try{
				languageCode = detectLanguage(synthText);//Detects language
				if(languageCode == null){
					languageCode = "en-us";//Reverts to Default Language if it can't detect it.
				}
			}
			catch(Exception ex){
				ex.printStackTrace();
				languageCode = "en-us";//Reverts to Default Language if it can't detect it.
			}
		}

		if(synthText.length()>100){
			List<String> fragments = parseString(synthText);//parses String if too long
			String tmp = getLanguage();
			setLanguage(languageCode);//Keeps it from autodetecting each fragment.
			InputStream out = getMP3Data(fragments);
			setLanguage(tmp);//Reverts it to it's previous Language such as auto.
			return out;
		}


		String encoded = URLEncoder.encode(synthText, "UTF-8"); //Encode

		URL url = new URL(GOOGLE_SYNTHESISER_URL + languageCode + "&q=" + encoded); //create url

		// Open New URL connection channel.
		URLConnection urlConn = url.openConnection(); //Open connection


		urlConn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:2.0) Gecko/20100101 Firefox/4.0"); //Adding header for user agent is required

		return urlConn.getInputStream();
	}

	/**
	 * Gets an InputStream to MP3Data for the returned information from a request
	 * @param synthText List of Strings you want to be synthesized into MP3 data
	 * @return Returns an input stream of all the MP3 data that is returned from Google
	 * @throws Exception Throws exception if it cannot complete the request
	 */
	public InputStream getMP3Data(List<String> synthText) throws Exception{
		InputStream complete = getMP3Data(synthText.remove(0));
		for(String part: synthText){
			complete = new java.io.SequenceInputStream(complete, getMP3Data(part));//Concatenate with new MP3 Data
		}
		return complete;
	}

	/**
	 * Separates a string into smaller parts so that Google will not reject the request.
	 * @param input The string you want to separate
	 * @return A List<String> of the String fragments from your input..
	 */
	private List<String> parseString(String input){
		return parseString (input, new ArrayList<String>());
	}

	/**
	 * Separates a string into smaller parts so that Google will not reject the request.
	 * @param input The string you want to break up into smaller parts
	 * @param fragments List<String> that you want to add stuff too.
	 * If you don't have a List<String> already constructed "new ArrayList<String>()" works well.
	 * @return A list of the fragments of the original String
	 */
	private List<String> parseString(String input, List<String> fragments){
		if(input.length()<=100){//Base Case
			fragments.add(input);
			return fragments;
		}
		else{
			int lastWord = findLastWord(input);//Checks if a space exists
			if(lastWord<=0){
				fragments.add(input.substring(0,100));//In case you sent gibberish to Google.
				return parseString(input.substring(100), fragments);
			}else{
				fragments.add(input.substring(0,lastWord));//Otherwise, adds the last word to the list for recursion.
				return parseString(input.substring(lastWord), fragments);
			}
		}
	}

	/**
	 * Finds the last word in your String (before the index of 99) by searching for spaces and ending punctuation.
	 * Will preferably parse on punctuation to alleviate mid-sentence pausing
	 * @param input The String you want to search through.
	 * @return The index of where the last word of the string ends before the index of 99.
	 */
	private int findLastWord(String input){
		if(input.length()<100)
			return input.length();
		int space = -1;
		for(int i = 99; i>0; i--){
			char tmp = input.charAt(i);
			if(isEndingPunctuation(tmp)){
				return i+1;
			}
			if(space==-1 && tmp == ' '){
				space = i;
			}
		}
		if(space>0){
			return space;
		}
		return -1;
	}

	/**
	 * Checks if char is an ending character
	 * Ending punctuation for all languages according to Wikipedia (Except for Sanskrit non-unicode)
	 * @param The char you want check
	 * @return True if it is, false if not.
	 */
	private boolean isEndingPunctuation(char input){
		return input == '.' || input == '!' || input == '?' || input == ';' || input == ':' || input == '|';
	}

	/**
	 * Automatically determines the language of the original text
	 * @param text represents the text you want to check the language of
	 * @return the languageCode in ISO-639
	 * @throws Exception if it cannot complete the request
	 */
	public String detectLanguage(String text) throws Exception{
		return GoogleTranslate.detectLanguage(text);
	}
}

