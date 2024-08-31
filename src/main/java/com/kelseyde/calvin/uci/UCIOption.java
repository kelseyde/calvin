package com.kelseyde.calvin.uci;

import com.kelseyde.calvin.engine.EngineConfig;

import java.util.function.Consumer;

public abstract class UCIOption {

    private String name;

    private Consumer<EngineConfig> consumer;

    public abstract void print();

    public class Check extends UCIOption {

            private boolean defaultValue;

            @Override
            public void print() {
                UCI.write("option name " + name + " type check default " + defaultValue);
            }

    }

    public class Spin extends UCIOption {

        private int defaultValue;
        private int min;
        private int max;
        private int step;

        @Override
        public void print() {
            UCI.write("option name " + name + " type spin default " + defaultValue + " min " + min + " max " + max);
        }
    }

    public class StringValue extends UCIOption {

        private String defaultValue;

        @Override
        public void print() {
            UCI.write("option name " + name + " type string default " + defaultValue);
        }
    }

}
