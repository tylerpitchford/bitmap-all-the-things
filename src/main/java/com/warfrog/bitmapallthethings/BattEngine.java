/*
*
* BitTwiddler - BMP transcoder
* Copyright (C) 2015  Tyler Pitchford
*
* This file is part of BitTwiddler.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; see the file COPYING.  If not, write to
* the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
*
*/

package com.warfrog.bitmapallthethings;

import org.apache.commons.cli.*;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.EndianUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class BattEngine {

    private static final String ENCODE = "encode";
    private static final String DECODE = "decode";

    //bitmap settings
    private File inputTarget = null;
    private String outputDirectory = ".";
    private String action = null;
    private String extensionFilter = null;
    private int width = 4000;
    private int height = 4000;
    private long maxFileSize = 64000000;
    private int bytesPerPixel = 32;

    //rar settings
    private boolean performRar = false;
    private String rarLocation = null;
    private String rarPassword = null;
    private String rarName = null;
    private int rarCompression = 0;
    private int rarRecoveryRecord = 10;

    //integration settings
    private boolean suppressHelp = false;
    private boolean cleanUp = false;

    private File getInputTarget() {
        return inputTarget;
    }

    private void setInputTarget(File inputTarget) {
        this.inputTarget = inputTarget;
    }

    private String getAction() {
        return action;
    }

    private void setAction(String action) {
        this.action = action;
    }

    private String getExtensionFilter() {
        return extensionFilter;
    }

    private void setExtensionFilter(String extensionFilter) {
        //remove any *.
        if (extensionFilter.startsWith("*.")) {
            extensionFilter = extensionFilter.substring(2);
        }
        this.extensionFilter = extensionFilter;
    }

    private int getWidth() {
        return width;
    }

    private void setWidth(int width) {
        this.width = width;
    }

    private int getHeight() {
        return height;
    }

    private void setHeight(int height) {
        this.height = height;
    }

    private long getMaxFileSize() {
        return maxFileSize;
    }

    private void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    private int getBytesPerPixel() {
        return bytesPerPixel;
    }

    private void setBytesPerPixel(int bytesPerPixel) {
        this.bytesPerPixel = bytesPerPixel;
    }

    private String getOutputDirectory() {
        return outputDirectory;
    }

    private void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    private boolean isPerformRar() {
        return performRar;
    }

    private void setPerformRar(boolean performRar) {
        this.performRar = performRar;
    }

    public String getRarPassword() {
        return rarPassword;
    }

    public void setRarPassword(String rarPassword) {
        this.rarPassword = rarPassword;
    }

    public String getRarLocation() {
        return rarLocation;
    }

    public void setRarLocation(String rarLocation) {
        this.rarLocation = rarLocation;
    }

    private String getRarName() {
        return rarName;
    }

    private void setRarName(String rarName) {
        //make sure we have a .rar extension
        this.rarName = rarName;
        if (!getRarName().endsWith(".rar")) {
            setRarName(getRarName() + ".rar");
        }
    }

    private int getRarCompression() {
        return rarCompression;
    }

    private void setRarCompression(int rarCompression) {
        this.rarCompression = rarCompression;
    }

    private int getRarRecoveryRecord() {
        return rarRecoveryRecord;
    }

    private void setRarRecoveryRecord(int rarRecoveryRecord) {
        this.rarRecoveryRecord = rarRecoveryRecord;
    }

    private boolean isSuppressHelp() {
        return suppressHelp;
    }

    private void setSuppressHelp(boolean suppressHelp) {
        this.suppressHelp = suppressHelp;
    }

    private boolean isCleanUp() {
        return cleanUp;
    }

    private void setCleanUp(boolean cleanUp) {
        this.cleanUp = cleanUp;
    }

    private void parseHelpOption(CommandLine line, Options options) {
        if (line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("bitmapallthethings", options);
            System.exit(0);
        }
    }

    private Options setupCommandLineOptions() {
        Options options = new Options();
        options.addOption("?", "help", false, "Prints this help message");
        options.addOption("a", "action", true, "Sets the transcoder action. Supported values are encode or decode (required)");
        options.addOption("w", "width", true, "Set the image width (defaults to 4000)");
        options.addOption("h", "height", true, "Set the image height (defaults to 4000)");
        options.addOption("m", "max_file_size", true, "Set the max file size in bytes (defaults to 64000000");
        options.addOption("b", "bytes_per_pixel", true, "Set the number of bits per pixel. Supported values are 8,16,24,32. (Default is 32)");
        options.addOption("e", "extension_filter", true, "Set the extension filter");
        options.addOption("i", "input", true, "Specifies the input target, can be either a file or a folder (required)");
        options.addOption("o", "output", true, "Specifies the output directory (defaults to .)");
        options.addOption("s", "suppress_help", false, "Suppresses the help output when there is a command line parsing error.");
        options.addOption("c", "clean_up", false, "Delete temporary files.");
        options.addOption("r", "rar", false, "Will attempt to execute rar if it is found on the system path (valid for both encode and decode).");
        options.addOption("rl", "rar_location", true, "Directory were the rar executable can be found.");
        options.addOption("rn", "rar_name", true, "Set the name of the rar archive.");
        options.addOption("rx", "rar_compression", true, "Set the amount of compression for rar (values are 0 - 5; default 0).");
        options.addOption("rr", "rar_recovery", true, "Set the percentage of recovery record data for rar (values are 0 - 100; default 10).");
        options.addOption("rp", "rar_password", true, "Set a password and encrypt the rar files.");
        return options;
    }

    private boolean parseActionOption(CommandLine line) {
        boolean parsingError = false;
        // validate the action value
        if (line.hasOption("action")) {
            setAction(line.getOptionValue("action"));
            //sanity check
            if (!getAction().equalsIgnoreCase("encode") && !action.equalsIgnoreCase("decode")) {
                // oops, something went wrong
                System.err.println("You must provide an action value of either 'encode' or 'decode'.");
                parsingError = true;
            }
        } else {
            System.err.println("You must provide an action value of either 'encode' or 'decode'.");
            parsingError = true;
        }
        return parsingError;
    }

    private boolean parseWidthOption(CommandLine line) {
        boolean parsingError = false;
        //validate the width value
        if (line.hasOption("width")) {
            String input = line.getOptionValue("width");
            try {
                setWidth(Integer.parseInt(input));

                if (getWidth() > 32000) {
                    System.err.println("Invalid width, valid width values for the BMP format are 0 to 32000.");
                    parsingError = true;
                }

            } catch (NumberFormatException ex) {
                System.err.println("You must provide a proper integer value for width.");
                parsingError = true;
            }
        }
        return parsingError;
    }

    private boolean parseHeightOption(CommandLine line) {
        boolean parsingError = false;
        //validate the height value
        if (line.hasOption("height")) {
            String input = line.getOptionValue("height");
            try {
                setHeight(Integer.parseInt(input));

                if (getHeight() > 32000) {
                    System.err.println("Invalid height, valid height values for the BMP format are 0 to 32000.");
                    parsingError = true;
                }

            } catch (NumberFormatException ex) {
                System.err.println("You must provide a proper integer value for height.");
                parsingError = true;
            }
        }
        return parsingError;
    }

    private boolean parseMaxFileSizeOption(CommandLine line) {
        boolean parsingError = false;
        //validate the max_file_size value
        if (line.hasOption("max_file_size")) {
            String input = line.getOptionValue("max_file_size");
            try {
                setMaxFileSize(Long.parseLong(input));
            } catch (NumberFormatException ex) {
                System.err.println("You must provide a proper long value for max_file_size.");
                parsingError = true;
            }
        }
        return parsingError;
    }

    private boolean parseBytesPerPixelOption(CommandLine line) {
        boolean parsingError = false;
        //validate the max_file_size value
        if (line.hasOption("bytes_per_pixel")) {
            String input = line.getOptionValue("bytes_per_pixel");
            try {
                setBytesPerPixel(Integer.parseInt(input));
            } catch (NumberFormatException ex) {
                System.err.println("You must provide a value of 8, 16, 24, or 32 for bytes_per_pixel.");
                parsingError = true;
            }

            if (getBytesPerPixel() != 8 && getBytesPerPixel() != 16 && getBytesPerPixel() != 24 && getBytesPerPixel() != 32) {
                System.err.println("You must provide a value of 8, 16, 24, or 32 for bytes_per_pixel.");
                parsingError = true;
            }
        }
        return parsingError;
    }

    private boolean parseExtensionFilterOption(CommandLine line) {
        boolean parsingError = false;
        //validate the extension value
        if (line.hasOption("extension_filter")) {
            setExtensionFilter(line.getOptionValue("extension_filter"));
        }
        return parsingError;
    }

    private boolean parseInputTargetOption(CommandLine line) {
        boolean parsingError = false;
        //validate the input value
        if (line.hasOption("input")) {
            setInputTarget(new File(line.getOptionValue("input")));
            if (!inputTarget.exists()) {
                System.err.println("Could not open the input specified -- file not found.");
                parsingError = true;
            }

        } else {
            System.err.println("You must provide either a file or directory for the input value.");
            parsingError = true;
        }
        return parsingError;
    }

    private boolean parseOutputDirectoryOption(CommandLine line) {
        boolean parsingError = false;
        if (line.hasOption("output")) {
            setOutputDirectory(line.getOptionValue("output"));
        }
        return parsingError;
    }

    private boolean parseSuppressHelpMessageOption(CommandLine line) {
        boolean parsingError = false;
        if (line.hasOption("suppress_help")) {
            setSuppressHelp(true);
        }
        return parsingError;
    }

    private boolean parseCleanUpOption(CommandLine line) {
        boolean parsingError = false;
        if (line.hasOption("clean_up")) {
            setCleanUp(true);
        }
        return parsingError;
    }

    private boolean parseRarPasswordOption(CommandLine line) {
        boolean parsingError = false;
        if (line.hasOption("rar_password")) {
            setRarPassword(line.getOptionValue("rar_password"));
        }
        return parsingError;
    }

    private boolean parseRarLocationOption(CommandLine line) {
        boolean parsingError = false;
        if (line.hasOption("rar_location")) {
            setRarLocation(line.getOptionValue("rar_location"));
            //validate that rar is there
            if(!RarUtility.isRarAvailable(getRarLocation())) {
                System.err.println("Rar not found at " + getRarLocation() + ".");
                parsingError = true;
            }
        }
        return parsingError;
    }

    private boolean parseRarNameOption(CommandLine line) {
        boolean parsingError = false;
        if (line.hasOption("rar_name")) {
            setRarName(line.getOptionValue("rar_name"));
        }
        return parsingError;
    }

    private boolean parseRarCompressionOption(CommandLine line) {
        boolean parsingError = false;
        if (line.hasOption("rar_compression")) {
            String input = line.getOptionValue("rar_compression");
            try {
                setRarCompression(Integer.parseInt(input));
            } catch (NumberFormatException ex) {
                System.err.println("You must provide an integer value for rar_compression.");
                parsingError = true;
            }

            if (getRarCompression() < 0 && getRarCompression() > 5) {
                System.err.println("You must provide a value between 0 and 5 for rar_compression.");
                parsingError = true;
            }
        }
        return parsingError;
    }

    private boolean parseRarRecoveryOption(CommandLine line) {
        boolean parsingError = false;
        if (line.hasOption("rar_recovery")) {
            String input = line.getOptionValue("rar_recovery");
            try {
                setRarRecoveryRecord(Integer.parseInt(input));
            } catch (NumberFormatException ex) {
                System.err.println("You must provide an integer value for rar_recovery.");
                parsingError = true;
            }

            if (getRarRecoveryRecord() < 0 && getRarRecoveryRecord() > 100) {
                System.err.println("You must provide a value between 0 and 10 for rar_recovery.");
                parsingError = true;
            }
        }
        return parsingError;
    }

    private boolean parseRarOption(CommandLine line) {
        boolean parsingError = false;
        if (line.hasOption("rar")) {
            if (RarUtility.isRarAvailable()) {
                setPerformRar(true);
            } else {
                System.err.println("Rar was not found in the current system path, " +
                        "download rar from http://www.win-rar.com/download.html to enable these features.");
                parsingError = true;
            }
        }
        return parsingError;
    }

    private void handleParsingErrors(boolean parsingError, Options options) throws Exception {
        if (parsingError) {
            System.out.print('\n');

            if (!isSuppressHelp()) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("bitmapallthethings", options);
            }
            throw new Exception("There were errors parsing the command line (see above).");
        }
    }

    private void parseCommandline(String args[]) throws Exception {

        // create the command line parser
        CommandLineParser parser = new DefaultParser();

        Options options = setupCommandLineOptions();

        boolean parsingError = false;
        CommandLine line = null;
        try {
            // parse the command line arguments
            line = parser.parse(options, args);
        } catch (ParseException exp) {
            System.out.println("Unexpected exception:" + exp.getMessage());
            parsingError = true;
        }

        parseHelpOption(line, options);
        parsingError |= parseActionOption(line);
        parsingError |= parseWidthOption(line);
        parsingError |= parseHeightOption(line);
        parsingError |= parseMaxFileSizeOption(line);
        parsingError |= parseBytesPerPixelOption(line);
        parsingError |= parseExtensionFilterOption(line);
        parsingError |= parseInputTargetOption(line);
        parsingError |= parseOutputDirectoryOption(line);
        parsingError |= parseSuppressHelpMessageOption(line);
        parsingError |= parseCleanUpOption(line);
        parsingError |= parseRarOption(line);

        //only parse these if rar is enabled
        if (isPerformRar()) {
            parsingError |= parseRarNameOption(line);
            parsingError |= parseRarCompressionOption(line);
            parsingError |= parseRarRecoveryOption(line);
            parsingError |= parseRarLocationOption(line);
            parsingError |= parseRarPasswordOption(line);
        }

        handleParsingErrors(parsingError, options);
    }

    private void printoutCurrentSettings() {
        //we have valid parameters
        System.out.println("Using the current values:");
        System.out.println("Action: " + getAction());
        System.out.println("Width: " + getWidth());
        System.out.println("Height: " + getHeight());
        System.out.println("Max file size: " + getMaxFileSize());
        System.out.println("Bytes per pixel " + getBytesPerPixel());
        System.out.println("Extension filter: " + getExtensionFilter());
        System.out.println("Input target: " + getInputTarget());
        System.out.println("Output directory: " + getOutputDirectory());
        System.out.println("Suppress help: " + isSuppressHelp());
        System.out.println("Clean up: " + isCleanUp());
        System.out.println("Perform rar : " + isPerformRar());
        System.out.println("Rar name : " + getRarName());
        System.out.println("Rar compression : " + getRarCompression());
        System.out.println("Rar recovery records : " + getRarRecoveryRecord());
        System.out.println("Rar locations : " + getRarLocation());
        System.out.println("Rar password : " + getRarPassword());
    }

    private InputStream generateBitmapHeader(int width, int height, int fileSize, int fillerBytes) {
        ByteBuffer buffer = ByteBuffer.allocate(54);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 0x42);        //B
        buffer.put((byte) 0x4D);        //M
        buffer.putInt(fileSize + 54);        //total file size
        buffer.putInt(fileSize);        //unofficial -- used to save the file size
        buffer.putInt(54);               //pixel info offset
        buffer.putInt(40);              //size of the bitmap info header
        buffer.putInt(width);           //width
        buffer.putInt(height);          //height
        buffer.putShort((short) 1);     //number of color planes
        buffer.putShort((short) getBytesPerPixel());    //bytes per pixel
        buffer.putInt(0);               //no compression
        buffer.putInt(fileSize);       //size of the raw pixel array
        buffer.putInt(2835);            //horizontal resolution
        buffer.putInt(2835);            //vertical resolution
        buffer.putInt(0);               //number of colors
        buffer.putInt(0);               //important colors
        return new ByteArrayInputStream(buffer.array());
    }

    private InputStream generateFileInputStream(String filename) throws FileNotFoundException {
        return new FileInputStream(filename); //this should be read from the internal values
    }

    private InputStream generateFillerStream(int bytes) {
        return new ByteArrayInputStream(new byte[bytes]);
    }

    private void decodeBitmap(String filename) throws IOException {
        System.out.println("Decoding " + filename);

        File inputFile = new File(filename);
        File outputFile = new File(outputDirectory + File.separator + FilenameUtils.removeExtension(inputFile.getName()));

        FileInputStream fis = new FileInputStream(filename);
        //skip 6 bytes
        fis.skip(6);
        //read the length we encoded
        int fileSize = EndianUtils.readSwappedInteger(fis);
        //skip the rest of the header
        fis.skip(44);
        Files.copy(fis, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        //truncate the file
        FileChannel outChan = new FileOutputStream(outputFile, true).getChannel();
        outChan.truncate(fileSize);
        outChan.close();

        //clean up
        if (isCleanUp()) {
            //delete the bitmap
            System.out.println("Deleting: " + inputFile);
            FileUtils.deleteQuietly(inputFile);
        }
    }

    private void generateBitmap(String inputName, String outputName) throws Exception {
        System.out.println("Generating " + outputName);

        File input = new File(inputName);
        int size = (int) new FileInputStream(inputName).getChannel().size();

        if (size > getMaxFileSize()) {
            System.err.println("ERROR: Skipping " + inputName + " the file size is larger than the maximum size allowed.");
            return;
        }

        int height = (size / (getBytesPerPixel() / 8)) / getWidth();
        int fillerBytes = (size / (getBytesPerPixel() / 8)) % getWidth();

        //encode (repeat this for each file in a directory)
        InputStream header = generateBitmapHeader(getWidth(), height, size, fillerBytes);
        InputStream file = generateFileInputStream(inputName);
        InputStream filler = generateFillerStream(fillerBytes);

        Vector<InputStream> inputStreams = new Vector<InputStream>();
        inputStreams.add(header);
        inputStreams.add(file);
        inputStreams.add(filler);

        SequenceInputStream inputStream = new SequenceInputStream(inputStreams.elements());
        Files.copy(inputStream, new File(outputName).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void createOutputDirectory() {
        File output = new File(outputDirectory);
        if (!output.exists()) {
            output.mkdirs();
        }
    }

    private String determineRarFilename() {
        return (getRarName() != null) ? getRarName() : getInputTarget().getName() + ".rar";
    }

    private String generateRarInputTarget() {
        String returnValue = getInputTarget().getAbsolutePath();
        if (getInputTarget().isDirectory() && getExtensionFilter() != null && !getExtensionFilter().equalsIgnoreCase("*.*")) {
            //we have an extension
            returnValue += File.separator + "*." + getExtensionFilter();
        }
        return returnValue;
    }

    private List<String> generateRarCommand() {
        List<String> command = new ArrayList<String>();
        command.add("rar");
        command.add("a");
        command.add("-y");
        command.add("-ep1");
        command.add("-m" + getRarCompression());
        command.add("-rr" + getRarRecoveryRecord());
        command.add("-v" + getMaxFileSize() + "b");
        if(getRarPassword() != null && !getRarPassword().trim().isEmpty()) {
            command.add("-hp" + getRarPassword());
        }
        command.add(getOutputDirectory() + File.separator + determineRarFilename());
        command.add(generateRarInputTarget());
        return command;
    }

    private List<String> generateUnrarCommand(String filename) {
        List<String> command = new ArrayList<String>();
        command.add("rar");
        command.add("x");
        command.add("-y");
        if(getRarPassword() != null && !getRarPassword().trim().isEmpty()) {
            command.add("-hp" + getRarPassword());
        }
        command.add(filename);
        command.add(getOutputDirectory());
        return command;
    }

    private void executeRar() throws Exception {
        CommandUtility.executeCommand(generateRarCommand());
    }

    private File locateFirstRar() {
        File returnValue = null;

        String[] extensions = new String[]{"rar"};
        List<File> files = (List<File>) FileUtils.listFiles(new File(getOutputDirectory()), extensions, true);
        if (files.size() == 1) {
            returnValue = files.get(0);
        } else {
            for (File file : files) {
                if (file.getName().matches(".*part0*1\\.rar")) {
                    returnValue = file;
                    break;
                }
            }
        }

        return returnValue;
    }

    private void executeUnrar() throws Exception {
        //locate the proper rar
        File firstRar = locateFirstRar();
        if (firstRar != null) {
            CommandUtility.executeCommand(generateUnrarCommand(firstRar.getAbsolutePath()));
        } else {
            System.out.println("Could not locate a rar to extract.");
        }
    }

    private String generateOutputName(String fileName) {
        return getOutputDirectory() + File.separator + fileName;
    }

    private void performDecode() throws Exception {
        if (isInputTargetADirectory()) {
            String extension = "bmp";
            String[] extensions = new String[]{extension};
            List<File> files = (List<File>) FileUtils.listFiles(getInputTarget(), extensions, true);
            for (File file : files) {
                decodeBitmap(file.getAbsolutePath());
            }
        } else {
            //process a single file
            decodeBitmap(getInputTarget().getAbsolutePath());
        }
    }

    private void performEncoding() throws Exception {

        if (isInputTargetADirectory()) {
            String extension = "*";
            if (getExtensionFilter() != null) {
                extension = getExtensionFilter();
            }
            String[] extensions = new String[]{extension};
            List<File> files = (List<File>) FileUtils.listFiles(getInputTarget(), extensions, true);
            for (File file : files) {
                generateBitmap(file.getAbsolutePath(), generateOutputName(file.getName() + ".bmp"));
                if(isCleanUp() && isPerformRar()) {
                    System.out.println("Deleting " + file.getAbsolutePath());
                    FileUtils.deleteQuietly(file);
                }
            }
        } else {
            //process a single file
            generateBitmap(getInputTarget().getAbsolutePath(), generateOutputName(getInputTarget().getName()));
        }
    }

    private boolean isInputTargetADirectory() {
        return getInputTarget().isDirectory();
    }

    private boolean isEncode() {
        boolean returnValue = false;
        if (getAction().equalsIgnoreCase("encode")) {
            returnValue = true;
        }
        return returnValue;
    }

    private void removeRars() {
        removeFiles("rar");
    }

    private void removeFiles(String extension) {
        String[] extensions = new String[]{extension};
        List<File> files = (List<File>) FileUtils.listFiles(new File(getOutputDirectory()), extensions, true);
        for (File file : files) {
            System.out.println("Deleting " + file.getAbsolutePath());
            FileUtils.deleteQuietly(file);
        }
    }

    private void unzipArchive() throws Exception {
        System.out.println("Unzipping " + getInputTarget());

        final InputStream is = new FileInputStream(getInputTarget());
        final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream("zip", is);

        ZipArchiveEntry entry = null;
        while ((entry = (ZipArchiveEntry) in.getNextEntry()) != null) {
            System.out.println("Extracting " + entry.getName());
            final OutputStream out = new FileOutputStream(new File(getOutputDirectory(), entry.getName()));
            IOUtils.copy(in, out);
            out.close();
        }
        in.close();
    }

    private void performRar() throws Exception {
        if (isPerformRar() && RarUtility.isRarAvailable()) {
            executeRar();
            //change the input to the newly created rars
            //add the extension filter
            setInputTarget(new File(getOutputDirectory()));
            setExtensionFilter("rar");
        }
    }

    private void performUnzip() throws Exception {
        if (getInputTarget().getName().endsWith(".zip")) {
            unzipArchive();
            if (isCleanUp()) {
                System.out.println("Deleting " + getInputTarget());
                //delete the zip
                FileUtils.deleteQuietly(getInputTarget());
            }
            //switch the input target to the output directory
            setInputTarget(new File(getOutputDirectory()));
        }
    }

    private void extractRars() throws Exception {
        if (isPerformRar() && RarUtility.isRarAvailable()) {
            executeUnrar();
            if (isCleanUp()) {
                //delete the rars
                removeRars();
            }
        }
    }

    private void handleDecodeRequest() throws Exception {
        performUnzip();
        performDecode();
        extractRars();
    }

    private void handleEncodeRequest() throws Exception {
        performRar();
        performEncoding();
    }

    public void start(String[] args) throws Exception {
        parseCommandline(args);
        printoutCurrentSettings();
        createOutputDirectory();
        //here's the meat
        if (isEncode()) {
            handleEncodeRequest();
        } else {
            //we only have two actions, so it must be a decode
            handleDecodeRequest();
        }
    }
}
