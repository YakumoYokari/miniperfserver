package com.github.sandin.miniperf.server.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Argument Parser
 */
public final class ArgumentParser {

    public static class Argument {
        private String key;
        private String help;
        private Object value;
        private boolean hasValue = true;

        public Argument(String key, String help, Object value, boolean hasValue) {
            this.key = key;
            this.help = help;
            this.value = value;
            this.hasValue = hasValue;
        }

        @Override
        public String toString() {
            return "Argument{" +
                    "key='" + key + '\'' +
                    ", help='" + help + '\'' +
                    ", value=" + value +
                    ", hasValue=" + hasValue +
                    '}';
        }
    }

    public static class Arguments {
        private List<Argument> mArguments = new ArrayList<>();

        public Argument get(String key) {
            if (key != null) {
                for (Argument argument : mArguments) {
                    if (key.equals(argument.key)) {
                        return argument;
                    }
                }
            }
            return null;
        }

        public String getAsString(String key, String defVal) {
            Argument argument = this.get(key);
            if (argument != null) {
                return (String)argument.value;
            }
            return defVal;
        }

        public boolean has(String key) {
            Argument arg = this.get(key);
            return arg != null && arg.value != null;
        }

        public void add(Argument arg) {
            mArguments.add(arg);
        }

        @Override
        public String toString() {
            return mArguments.toString();
        }
    }

    private Arguments mArguments = new Arguments();

    public ArgumentParser() {
    }

    public ArgumentParser addArg(String key, String help, boolean hasValue) {
        mArguments.add(new Argument(key, help, null, hasValue));
        return this;
    }

    public Arguments parse(String[] args) {
        Argument expectValueArgument = null;
        for (String arg : args) {
            if (expectValueArgument != null) { // It is a value
                expectValueArgument.value = arg;
            } else { // maybe it's a key
                String key = null;
                if (arg.startsWith("--")) {
                    key = arg.substring(2);
                } else if (arg.startsWith("-")) {
                    key = arg.substring(1);
                }
                if (key != null) {
                    Argument argument = mArguments.get(key);
                    //System.out.println("Got a key: " + key + ", argument: " + argument);
                    if (argument != null) {
                        if (argument.hasValue) {
                            expectValueArgument = argument;
                        } else {
                            argument.value = true; // no value, just set a flag
                            expectValueArgument = null;
                        }
                    }
                }
            }
        }
        return mArguments;
    }

}
