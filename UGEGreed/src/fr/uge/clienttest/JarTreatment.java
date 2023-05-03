package fr.uge.clienttest;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.ugegreed.Checker;

public class JarTreatment {
	private static final Logger logger = Logger.getLogger(JarTreatment.class.getName());
	
	/**
	 *  Check if the string parsed is an url
	 * @param strUrl
	 * @return
	 */
	public static boolean isURL(String strUrl) {
		try {
			new URL(strUrl);
			return true;
		}catch(MalformedURLException e) {
			return false;
		}
	}
	
	/**
	 * Check if the string parsed is a path
	 * @param strPath
	 * @return
	 */
	public static boolean isPath(String strPath) {
		try {
			Paths.get(strPath);
			return true;
		}catch(InvalidPathException e) {
			return false;
		}
	}
	
	/**
	 * Check if the string parsed is a file
	 * @param strFile
	 * @return
	 */
	public static boolean isFile(String strFile) {
		File file = new File(strFile);
		if(isPath(strFile)) {
			if(file.exists()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 *  Make the checker on the values and put result in the specified file
	 * @param checker
	 * @param startValue
	 * @param endValue
	 * @param path
	 */
	public static void checkOnValues(Checker checker,Long startValue,Long endValue,Path path) {
		if(!Files.exists(path)) {
			try {
				Files.createFile(path);
			} catch (IOException e) { 
				logger.log(Level.WARNING, "Cannot create the file");
			}
		}
		try {
			for(var i = startValue; i <= endValue;i++) {
				//System.out.println(checker.check(i));
				Files.write(path, checker.check(i).getBytes(StandardCharsets.UTF_8),StandardOpenOption.APPEND);
				Files.write(path, "\n".getBytes(StandardCharsets.UTF_8),StandardOpenOption.APPEND);
			}
		} catch (InterruptedException e) {
			logger.log(Level.FINE, "Interrupted");
		} catch(IOException e) {
			logger.log(Level.WARNING, "IOException");
		}
	}
	
	/**
	 * Create the checker and get the JAR from url
	 * @param urlJar
	 * @param qualfiedName
	 * @param startValue
	 * @param endValue
	 * @param path
	 */
	public static void loopOnCheckerURL(String urlJar,String qualfiedName,Long startValue,Long endValue, Path path) {
		logger.info("Inputed an URL");
		Checker checker = Client.checkerFromHTTP(urlJar, qualfiedName).orElseThrow();
		checkOnValues(checker, startValue, endValue, path);
	}
	
	/**
	 *  Create the checker and get the JAR from disk space
	 * @param urlJar
	 * @param qualfiedName
	 * @param startValue
	 * @param endValue
	 * @param path
	 */
	public static void loopOnCheckerFile(String urlJar,String qualfiedName,Long startValue,Long endValue, Path path) {
		logger.info("Inputed a File");
		Checker checker = Client.checkerFromDisk(Path.of(urlJar), qualfiedName).orElseThrow();
		checkOnValues(checker, startValue, endValue, path);
	}
	
	public static void main(String[] args) {
		if(args.length!= 5) {
			System.out.println("USAGE : java JarTreatment urlJar fully-qualified-name start-range end-range filename");
			return;
		}
		String urlJar = args[0];
		String qualifiedName = args[1];
		Long startRange = Long.parseLong(args[2]);
		Long endRange = Long.parseLong(args[3]);
		Path path = Paths.get(args[4]);
		
		if(isURL(urlJar)) {
			loopOnCheckerURL(urlJar, qualifiedName, startRange, endRange, path);
		}
		else if(isFile(urlJar)) {
			loopOnCheckerFile(urlJar, qualifiedName, startRange, endRange, path);
		}
		else {
			logger.info("Is not a file or an url");
			return;
		}
		
	}
}
