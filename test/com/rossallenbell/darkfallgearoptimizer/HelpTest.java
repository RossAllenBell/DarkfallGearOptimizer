package com.rossallenbell.darkfallgearoptimizer;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HelpTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void testHelpFlagShortForm() throws Exception {
        String[] args = {"-h"};
        DarkfallGearOptimizer.main(args);

        String output = outContent.toString();
        assertTrue("Help should contain usage", output.contains("USAGE:"));
        assertTrue("Help should contain options", output.contains("OPTIONS:"));
        assertTrue("Help should contain examples", output.contains("EXAMPLES:"));
        assertTrue("Help should contain protection types", output.contains("PROTECTION TYPES:"));
    }

    @Test
    public void testHelpFlagLongForm() throws Exception {
        outContent.reset();
        String[] args = {"--help"};
        DarkfallGearOptimizer.main(args);

        String output = outContent.toString();
        assertTrue("Help should contain usage", output.contains("USAGE:"));
        assertTrue("Help should contain options", output.contains("OPTIONS:"));
        assertTrue("Help should contain examples", output.contains("EXAMPLES:"));
    }

    @Test
    public void testHelpContainsAllOptions() throws Exception {
        outContent.reset();
        String[] args = {"-h"};
        DarkfallGearOptimizer.main(args);

        String output = outContent.toString();
        assertTrue("Help should mention --format", output.contains("--format"));
        assertTrue("Help should mention --weight", output.contains("--weight"));
        assertTrue("Help should mention --threads", output.contains("--threads"));
        assertTrue("Help should mention --generate-presets", output.contains("--generate-presets"));
        assertTrue("Help should mention --use-legacy", output.contains("--use-legacy"));
    }

    @Test
    public void testHelpContainsProtectionTypes() throws Exception {
        outContent.reset();
        String[] args = {"--help"};
        DarkfallGearOptimizer.main(args);

        String output = outContent.toString();
        assertTrue("Help should mention slashing", output.contains("slashing"));
        assertTrue("Help should mention fire", output.contains("fire"));
        assertTrue("Help should mention bludgeoning", output.contains("bludgeoning"));
    }

    @Test
    public void testHelpContainsDatasets() throws Exception {
        outContent.reset();
        String[] args = {"-h"};
        DarkfallGearOptimizer.main(args);

        String output = outContent.toString();
        assertTrue("Help should mention minimal dataset", output.contains("armor-data-minimal"));
        assertTrue("Help should mention common dataset", output.contains("armor-data-common"));
        assertTrue("Help should mention complete dataset", output.contains("armor-data-complete"));
    }
}
