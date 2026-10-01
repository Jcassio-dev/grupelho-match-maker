package com.grupelho;

import com.grupelho.cli.StubTournamentRunner;
import com.grupelho.config.Config;

public class App {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("stub-demo")) {
            StubTournamentRunner.run(new Config());
            return;
        }

        System.out.println("grupelho - scaffold ok (fase 1)");
    }
}
