<p align="center"><img src="https://github.com/alterdekim/JMinecraftLauncher/blob/main/cli.gif?raw=true" width="400px" alt="cli"></p>

<h1 align="center">JML (Java Minecraft Launcher)</h1>
Yet another console-based Minecraft launcher.

```
java -jar jml.jar -gameDir=C:\Users\xxx\AppData\Roaming\.minecraft -gameVer="1.7.10" -player=Notch -lxms=1024M -lxmx=2048M
```

## Arguments

| Argument | Description | Optional |
| --- | --- | --- |
| -gameDir | Directory of game files | false |
| -gameVer | Game version | false |
| -player | Player name (offline) | false |
| -lxms | Alloc memory to Minecraft JVM instance | true |
| -lxmx | Max limit memory of Minecraft JVM instance | true |
| -jvm | Custom java path for Minecraft | true |
