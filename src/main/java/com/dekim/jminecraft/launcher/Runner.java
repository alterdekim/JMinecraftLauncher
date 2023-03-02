package com.dekim.jminecraft.launcher;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

import static com.dekim.jminecraft.launcher.Commands.*;

public class Runner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

    private final String[] args;

    private final InitialArguments arguments = new InitialArguments();

    public Runner( String[] args ) {
        this.args = args;
    }

    public void start() {
        LOGGER.info("Parsing cli arguments...");
        for( String arg : args ) {
            if( arg.startsWith(gameDir) ) {
                LOGGER.info("Game directory was set.");
                arguments.gameDir = arg.substring(gameDir.length());
            } else if( arg.startsWith(gameVer) ) {
                LOGGER.info("Game version was set.");
                arguments.gameVer = arg.substring(gameVer.length());
            } else if( arg.startsWith(playerName) ) {
                LOGGER.info("Player name was set.");
                arguments.playerName = arg.substring(playerName.length());
            } else if( arg.startsWith(gameDir+"\"") ) {
                LOGGER.info("Game directory was set.");
                arguments.gameDir = arg.substring(gameDir.length()).substring(0, arg.substring(gameDir.length()).indexOf("\""));
            } else if( arg.startsWith(gameVer+"\"") ) {
                LOGGER.info("Game version was set.");
                arguments.gameVer = arg.substring(gameVer.length()).substring(0, arg.substring(gameVer.length()).indexOf("\""));
            } else if( arg.startsWith(jvm) ) {
                LOGGER.info("Custom JVM was set.");
                arguments.jvm = arg.substring(jvm.length());
            } else if( arg.startsWith(jvm+"\"") ) {
                LOGGER.info("Custom JVM was set.");
                arguments.jvm = arg.substring(jvm.length());
            } else if( arg.startsWith(xms) ) {
                LOGGER.info("JVM Memory alloc was set.");
                arguments.xms = arg.substring(xms.length());
            } else if( arg.startsWith(xmx) ) {
                LOGGER.info("JVM Max memory alloc was set.");
                arguments.xmx = arg.substring(xmx.length());
            }
        }
        if(arguments.isFull()) {
            launch();
        } else {
            LOGGER.error("Not enough arguments.");
        }
    }

    private void launch() {
        try {
            StringBuilder sb = new StringBuilder();
            Files.readAllLines(Paths.get(arguments.gameDir, "versions", arguments.gameVer, arguments.gameVer + ".json")).forEach(sb::append);
            JSONObject config = new JSONObject(sb.toString());
            String minecraftArguments = config.get("minecraftArguments").toString();
            String mainClass = config.get("mainClass").toString();
            String type = config.get("type").toString();
            String assetsVer = arguments.gameVer;
            if( type.equals("modified") ) {
                assetsVer = config.get("jar").toString();
            }
            JSONArray libraries = config.getJSONArray("libraries");
            String cmd = "";
            arguments.jvm = arguments.jvm.contains(" ") ? "\"" + arguments.jvm + "\"" : arguments.jvm;
            cmd += arguments.jvm + " ";
            String xxargs = "-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:+ParallelRefProcEnabled ";
            cmd += xxargs;
            String jvmargs = "-Xms"+arguments.xms+" -Xmx"+arguments.xmx+" -Dfile.encoding=UTF-8 -Djava.net.useSystemProxies=true ";
            cmd += jvmargs;
            String fmlargs = "-Dfml.ignoreInvalidMinecraftCertificates=true -Dfml.ignorePatchDiscrepancies=true ";
            cmd += fmlargs;
            String npath = "-Djava.library.path=";
            cmd += npath;
            cmd += "\""+Paths.get(arguments.gameDir, "versions", arguments.gameVer, "natives") + "\" ";
            String mjarpath = "-Dminecraft.client.jar=";
            cmd += mjarpath;
            cmd += "\""+Paths.get(arguments.gameDir, "versions", arguments.gameVer, arguments.gameVer+".jar") + "\" ";
            cmd += "-cp ";
            StringBuilder ssb = new StringBuilder();
            for( int i = 0; i < libraries.length(); i++ ) {
                JSONObject jsonObject = libraries.getJSONObject(i);
                try {
                        String packName = jsonObject.get("name").toString();
                        String[] arr = packName.substring(0, packName.indexOf(":")).split("\\.");
                        StringBuilder rel_path = new StringBuilder();
                        Arrays.stream(arr).forEach(h -> rel_path.append(h).append("/"));
                        arr = packName.substring(packName.indexOf(":")+1).split(":");
                        Arrays.stream(arr).forEach(h -> rel_path.append(h).append("/"));
                        rel_path.append(arr[arr.length - 2]).append("-").append(arr[arr.length - 1]).append(".jar");
                        ssb.append("\"").append(Paths.get(arguments.gameDir, "libraries", rel_path.toString())).append("\";");
                } catch ( Exception e ) {
                    LOGGER.warn("Weird library found...");
                    e.printStackTrace();
                }
            }
            ssb.append("\"").append(Paths.get(arguments.gameDir, "versions", arguments.gameVer, arguments.gameVer + ".jar")).append("\" ");
            cmd += ssb.toString();
            cmd += mainClass + " ";
            cmd += getMinecraftArguments(minecraftArguments, assetsVer);
            Runtime r = Runtime.getRuntime();
            Process p = r.exec(cmd);
            BufferedReader br = p.inputReader();
            String s;
            while( (s = br.readLine()) != null ) {
                System.out.println(s);
            }
        } catch ( Exception e ) {
            LOGGER.error( e.getMessage() );
        }
    }

    private String getMinecraftArguments( String minecraftArguments, String assetsVer ) {
        minecraftArguments = minecraftArguments.replace("${auth_player_name}", arguments.playerName);
        minecraftArguments = minecraftArguments.replace("${version_name}", arguments.gameVer);
        minecraftArguments = minecraftArguments.replace("${game_directory}", arguments.gameDir);
        minecraftArguments = minecraftArguments.replace("${assets_root}", Paths.get(arguments.gameDir, "assets").toString());
        minecraftArguments = minecraftArguments.replace("${assets_index_name}", assetsVer);
        minecraftArguments = minecraftArguments.replace("${auth_uuid}", UUID.randomUUID().toString());
        minecraftArguments = minecraftArguments.replace("${auth_access_token}", UUID.randomUUID().toString());
        minecraftArguments = minecraftArguments.replace("${user_type}", "legacy");
        minecraftArguments = minecraftArguments.replace("${user_properties}", "{}");
        return minecraftArguments;
    }

    private static class InitialArguments {
        private String gameDir = "";
        private String gameVer = "";
        private String playerName = "";
        private String jvm = "java";
        private String xms = "2048M";
        private String xmx = "4096M";

        public boolean isFull() {
            return !(this.playerName.equals("") || this.gameDir.equals("") || this.gameVer.equals(""));
        }
    }

    public static void main(String[] args) {
        new Runner(args).start();
    }
}
