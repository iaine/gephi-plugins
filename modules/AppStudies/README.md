# AppStudies for Gephi

Import the JSON output of the **AppStudies** APK-analysis toolkit into [Gephi](https://gephi.org) as a graph, and use the **Timeline** to watch how an app changes from one version to the next.

---

## What it does

The AppStudies toolkit produces a JSON file for each app it analyses. This plugin reads those files and turns them into a graph you can explore in Gephi:

- **Single file** – import one `.json` or `.jsonl` file through Gephi's normal import dialog.
- **A whole folder** – point the plugin at a directory of files (one per app version) and it builds a single graph where every node and edge is tagged with the version it appeared in.
- **Version timeline** – because each element knows which versions it belongs to, Gephi's Timeline can play the graph forwards and backwards across versions, so you can see what each release added or removed.

The graph is built automatically from the analysis data. For "listening" analyses, for example, it links each native module to the audio-pipeline stage it belongs to (capture → dsp → features → inference → output).

---

## Installing

1. Download the plugin `.nbm` file (or build it from source – see below).
2. In Gephi, go to **Tools ▸ Plugins ▸ Downloaded ▸ Add Plugins…**
3. Select the `.nbm` file and follow the prompts, then restart Gephi when asked.

After restarting, you'll find the new menu items described below.

---

## Using it

### Import a single file

1. **File ▸ Open…** (or **File ▸ Import**).
2. Choose a `.json` or `.jsonl` file produced by the AppStudies toolkit.
3. Accept the import report. The graph appears in the **Overview** workspace.

### Import a whole folder (recommended for multiple versions)

1. **File ▸ Import AppStudies Directory…**
2. Pick the folder that contains your files. Each file should be one app version.
3. If you already have a graph open, you'll be asked:
   - **Yes – append** to merge the new files into the current graph (keeps what's already there).
   - **No – new graph** to start a clean workspace just for this import.
4. The plugin reads every `.json` / `.jsonl` file in the folder, sorts them by app version, and loads them into one graph.

> **Tip:** filenames don't have to be in any particular order – the plugin reads the version from inside each file and sorts automatically.

### Play the version timeline

After a folder import, the Gephi **Timeline** is switched on automatically and appears as a bar along the bottom of the window.

- Press the **play** button to animate the graph across versions.
- Drag the handles to focus on a range of versions.
- Elements fade in at the version where they first appear and stay visible in later versions.

For a readable list of which versions were loaded, open **Window ▸ AppStudies Version Timeline**. This shows each version (e.g. `43.7.3`) as a labelled tick, which is easier to read than the numbers Gephi's own timeline uses internally.

---

## Frequently asked questions

**Which files can I import?**
Any `.json` or `.jsonl` file from the AppStudies toolkit. Both the older "flows" format (with a ready-made graph) and the newer analysis formats (where the graph is built from the analysis data) are supported.

**Nothing happens / the graph is empty.**
The file may not contain graph or chain data for that analysis. Check the import report (it lists any files that were skipped and why).

**The timeline bar didn't appear.**
It only switches on automatically after a *folder* import. For a single-file import, turn it on with the small clock icon at the bottom-left of the Gephi window.

**Can I mix versions from different apps in one folder?**
It's best not to. Put one app's versions in a folder so the timeline reads as a single app's history. Different apps belong in separate imports (use the "new graph" option).

---

## Building from source

Requires JDK 17 and Maven, inside a checkout of the [gephi-plugins](https://github.com/gephi/gephi-plugins) repository.

```bash
# from the root of the gephi-plugins checkout
mvn package                                  # builds the plugin
mvn org.gephi:gephi-maven-plugin:run         # launches Gephi with it installed
```

The packaged `.nbm` is written to the module's `target/` folder.

---

## Support

Found a problem or have a request? Please open an issue on the project's GitHub page.
