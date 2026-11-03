<div align="center">

![Logo](https://cdn.modrinth.com/data/cached_images/94757f2e1868a2d9acdaf5d4d76b9b828aeb9f4b.png)
### Minecraft UI Enhancement Mod

Json Ui Engine for Minecraft Java Edition

[![Modrinth](https://img.shields.io/badge/Modrinth-Download-00AF5C?style=flat-square&logo=modrinth)](https://modrinth.com/mod/alan-unified-ui)
[![CurseForge](https://img.shields.io/badge/CurseForge-Download-F16436?style=flat-square&logo=curseforge)](https://www.curseforge.com/minecraft/mc-mods/alans-unified-ui)
[![GitHub](https://img.shields.io/badge/GitHub-Source-181717?style=flat-square&logo=github)](https://github.com/AlanHT520/SIRIUS-Ui-Mod)
[![License](https://img.shields.io/badge/License-GPLv3-blue?style=flat-square)](https://www.gnu.org/licenses/gpl-3.0.html)

</div>

---

## What is this?

SIRIUS Ui is a UI enhancement mod powered by a **DSL-driven Widget component system**. Game interfaces are written in **HT** (`.ht`) — a small, Minecraft-oriented UI language with templates, `extends`, typed variables, control flow and events. No Java code required.

```ht
screen title {
    image logo { image_texture = "sirius_ui:...png"; position(screen.width / 2 - this.width / 2, 30); size(256, 64); }
    logo.use();

    button play = primary_button;
    play.position(screen.width - this.width - 20, 100);
    play.on clicked { open_screen("selectGame"); }
    play.use();
}
```

Starting from version 0.5.0, SIRIUS Ui is open source under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html).

## Installation

1. Place the Mod JAR file into your `.minecraft/mods/` directory
2. Launch the game

## Author

**AlanHT520**

---

<div align="center">

</div>