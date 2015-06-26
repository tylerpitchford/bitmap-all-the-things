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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class CommandUtility {

    private static void buildEnvironment() {

    }

    public static void executeCommand(List<String> command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(command);

        System.out.println("Executing: " + pb.command());

        Process process = pb.start();

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(process.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(process.getErrorStream()));

        // read the output from the command
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        // read any errors from the attempted command
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }
    }

    public static boolean isCommandAvailable(String command) {
        boolean returnValue = false;

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process p = pb.start();
            returnValue = true;
        } catch (IOException ex) {
            //ex.printStackTrace();
        }

        return returnValue;
    }

}
