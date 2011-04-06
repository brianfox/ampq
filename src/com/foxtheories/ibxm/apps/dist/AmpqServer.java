/*
 * CVS LOG TRACKING
 * ----------------
 * $Log: AmpqServer.java,v $
 * Revision 1.5  2009/10/28 17:21:00  bfox
 * Stable code after many relocation changes.
 *
 * Revision 1.4  2009/10/27 17:13:28  bfox
 * Added CVS logging to everything.
 *
 * Revision 1.3  2009/10/27 17:09:15  bfox
 * CVS logging test.
 *
 */

package com.foxtheories.ibxm.apps.dist;

import ibxm.IBXM;
import ibxm.Module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.foxtheories.ibxm.amq.context.AmqProperties;
import com.foxtheories.ibxm.amq.threads.ServerThread;
import com.foxtheories.ibxm.sound.mixer.Mixer;
import com.foxtheories.ibxm.sound.mixer.Mixer.Strategy;
import com.foxtheories.util.GlobalLogging;

public class AmpqServer {

    private ServerThread sthread;
    private Logger logger;
    private BufferedReader reader;
    private Module module;
    private AmqProperties properties;


    public static void main(String[] args) {
        AmpqServer s = new AmpqServer();
        s.properties = new AmqProperties();
        s.properties.setAllProperty("broker", "192.168.148.128");
        s.run();
    }

    public AmpqServer() {
        InputStreamReader converter = new InputStreamReader(System.in);
        reader = new BufferedReader(converter);
    }

    private void run() {
        try {
            sthread = new ServerThread(properties, module);
            sthread.start();
            sthread.setLogLevel(Level.INFO);

            logger = GlobalLogging.createLog("com.foxtheories.ibmx.server", properties);
            Thread.sleep(2000);
            System.out.println("ampq_s is now running.\n");

            while (sthread.isAlive()) {
                System.out.printf("ServerCommandList: ");
                System.out.flush();
                if (nextCommand())
                    System.out.printf("%n%n");
                else
                    System.out.printf("Huh?  Try HELP. %n");    
            }
        } catch (Exception e) {
            System.err.println("An error occurred.  Could not process the command.");
            if (logger != null)
                GlobalLogging.pp(logger, Level.ERROR, e);
            else {
                System.err.println("Logging apparently isn't working either.  Aboring.");
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    private boolean nextCommand() throws InterruptedException, IOException {
        String x = reader.readLine().toLowerCase();
        ServerCommandList c = ServerCommandList.fromString(x);
        String[] tokens = x.split("\\s+");
        if (c == null)
            return false;
        
        switch (c) {

            case SHOW_CLIENTS:          showClients();     break;
            case SHOW_INSTRUMENTS:      showInstruments(); break;
            case SHOW_MODULE:           showModule();      break;
            case SHOW_CREDITS:          showCredits();     break;

            case LOAD:                  load(tokens);      break;
            case HELP:                  help();            break;
            case QUIT:                  quit();            break;

            case PLAY:                  sthread.play();    break;
            case PAUSE:                 sthread.pause();   break;
            case STOP:                  sthread.stopMod(); break;
            case RESET:                 sthread.reset();   break;

            case STRATEGY_BROADCAST:
            case STRATEGY_ROUND_ROBIN:
            case STRATEGY_INSTRUMENT:
            case STRATEGY_CHANNEL:      setStrategy(c);    break;

            default:
                return false;
        }
        return true;
    }

	private static void help() {
		System.out.println(ServerCommandList.help());
	}

	private static void showCredits() {
		System.out.printf("This application makes extensive use of the IBXM module%n");
		System.out.printf("player written by Martin Cameron and released under the%n");
		System.out.printf("BSD License.  IBXM originally used this VERSION string:%n");
		System.out.printf("%s", IBXM.VERSION);
	}

    private void showModule() {
        System.out.println(sthread.moduleList());
    }

    private void showInstruments() {
        System.out.println(sthread.instrumentList());
    }

    private void showClients() {
        System.out.println(sthread.clientList());
    }

    private void setStrategy(ServerCommandList c) {

        Strategy strat = null;
        switch (c) {
            case STRATEGY_BROADCAST:
                sthread.setStrategy(Mixer.Strategy.BROADCAST); break;
            case STRATEGY_ROUND_ROBIN:
                sthread.setStrategy(Mixer.Strategy.ROUND_ROBIN); break;
            case STRATEGY_CHANNEL:
                sthread.setStrategy(Mixer.Strategy.CHANNEL); break;
            case STRATEGY_INSTRUMENT:
                sthread.setStrategy(Mixer.Strategy.INSTRUMENT); break;
        }
        if (strat != null) {
            System.out.println("SETTING STRATEGY: " + strat);
            sthread.setStrategy(strat);
        }
    }

    private void load(String[] tokens) {
        String filename = tokens[1].substring(1,tokens[1].length()-1);
        System.out.println("LOADING: " + filename);
        try {
            Module yo = Module.load_module(filename);
            sthread.setModule(yo);
        }
        catch (IOException me1) {
            System.out.println("Could not load module: " + me1.getMessage());
        }
        catch (IllegalArgumentException me2) {
            System.out.println("Could not load module: " + me2.getMessage());
        }
        return;
    }

    private void quit() throws InterruptedException {
        sthread.quit();
        System.out.println("Shutting down.");
        while (sthread.isAlive())
            Thread.sleep(100);
        System.out.println("Goodbye.");
        System.exit(0);
    }

}



