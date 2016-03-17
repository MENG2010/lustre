package main;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import enums.Coverage;
import enums.Polarity;
import enums.Simulation;
import enums.Generation;

public final class LustreArgumentParser {
	private static final String HELP = "help";
	private static final String COVERAGE = "coverage";
	private static final String POLARITY = "polarity";
	private static final String GENERATE = "generate";
	private static final String SIMULATE = "simulate";
	private static final String MEASURE = "measure";
	private static final String NO_CSE = "no_cse";

	private final String name;
	private final LustreSettings settings;

	public static LustreSettings parse(String[] args) {
		LustreArgumentParser parser = new LustreArgumentParser();
		parser.parseArguments(args);
		parser.checkSettings();
		return parser.settings;
	}

	private LustreArgumentParser() {
		this.name = "LustreMain";
		this.settings = new LustreSettings();
	}

	private Options getOptions() {
		Options options = new Options();
		options.addOption(HELP, false, "print this message");
		/*
		 * Add "omcdc". Meng
		 */
		options.addOption(COVERAGE, true,
				"generate coverage obligations (mcdc, branch, condition, decision)");
		options.addOption(POLARITY, true,
				"polarity of generated coverage obligations (all, true, false)");
		options.addOption(GENERATE, true,
				"generate test cases, fill in don't care values (null, default, random)");
		options.addOption(SIMULATE, true,
				"execute a test suite (complete, partial)");
		options.addOption(
				MEASURE,
				true,
				"measure satisfaction of obligations (complete, partial), generate a reduced test suite");
		options.addOption(NO_CSE, false,
				"Disable common subexpression elimination");
		return options;
	}

	private void parseArguments(String[] args) {
		CommandLineParser parser = new BasicParser();
		CommandLine line = null;
		try {
			line = parser.parse(getOptions(), args);
		} catch (ParseException e) {
			LustreMain.error(e.getMessage());
		}

		if (line.hasOption(HELP)) {
			printHelp();
			System.exit(0);
		}

		String[] input = line.getArgs();

		if (input.length >= 1) {
			this.settings.program = input[0];
			if (input.length >= 2) {
				this.settings.tests = input[1];
				if (input.length >= 3) {
					this.settings.oracle = input[2];
				}
			}
		} else {
			printHelp();
			System.exit(0);
		}

		if (line.hasOption(COVERAGE)) {
			this.settings.coverage = this.getCoverage(line
					.getOptionValue(COVERAGE));
		}

		if (line.hasOption(POLARITY)) {
			this.settings.polarity = this.getPolarity(line
					.getOptionValue(POLARITY));
		}

		if (line.hasOption(GENERATE)) {
			this.settings.generation = this.getValueType(line
					.getOptionValue(GENERATE));
		}

		if (line.hasOption(SIMULATE)) {
			this.settings.simulation = this.getSimulation(line
					.getOptionValue(SIMULATE));
		}

		if (line.hasOption(MEASURE)) {
			this.settings.measure = this.getSimulation(line
					.getOptionValue(MEASURE));
		}

		if (line.hasOption(NO_CSE)) {
			this.settings.cse = false;
		}
	}

	private void checkSettings() {
		if (settings.polarity != null && settings.coverage == null) {
			LustreMain.error("polarity should be used with coverage");
		}
		if (settings.simulation != null && settings.measure != null) {
			LustreMain
					.error("simulate and measure should not be used together");
		}
		if (settings.generation != null && settings.tests == null) {
			LustreMain.error("generate should be used with a test suite");
		}
		if (settings.simulation != null && settings.tests == null) {
			LustreMain.error("simulate should be used with a test suite");
		}
		if (settings.measure != null && settings.tests == null) {
			LustreMain.error("measure should be used with a test suite");
		}
		if (settings.measure != null && settings.oracle != null) {
			LustreMain.error("measure should not be used with oracle");
		}
	}

	private Coverage getCoverage(String coverage) {
		List<Coverage> options = Arrays.asList(Coverage.values());
		for (Coverage option : options) {
			if (coverage.equals(option.toString())) {
				return option;
			}
		}
		LustreMain.error("unknown coverage: " + coverage + "\n"
				+ "Valid options: " + options);
		return null;
	}

	private Polarity getPolarity(String polarity) {
		List<Polarity> options = Arrays.asList(Polarity.values());
		for (Polarity option : options) {
			if (polarity.equals(option.toString())) {
				return option;
			}
		}
		LustreMain.error("unknown polarity: " + polarity + "\n"
				+ "Valid options: " + options);
		return null;
	}

	private Simulation getSimulation(String simulation) {
		List<Simulation> options = Arrays.asList(Simulation.values());
		for (Simulation option : options) {
			if (simulation.equals(option.toString())) {
				return option;
			}
		}
		LustreMain.error("unknown simulation: " + simulation + "\n"
				+ "Valid options: " + options);
		return null;
	}

	private Generation getValueType(String generation) {
		List<Generation> options = Arrays.asList(Generation.values());
		for (Generation option : options) {
			if (generation.equals(option.toString())) {
				return option;
			}
		}
		LustreMain.error("unknown generation: " + generation + "\n"
				+ "Valid options: " + options);
		return null;
	}

	private void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(this.name.toLowerCase()
				+ " [options] <program> [tests] [oracle]", getOptions());
	}
}
