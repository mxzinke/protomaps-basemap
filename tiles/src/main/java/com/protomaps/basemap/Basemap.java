package com.protomaps.basemap;

import com.onthegomap.planetiler.ForwardingProfile;
import com.onthegomap.planetiler.Planetiler;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.util.Downloader;
import com.protomaps.basemap.feature.NaturalEarthDb;
import com.protomaps.basemap.feature.QrankDb;
import com.protomaps.basemap.layers.Earth;
import com.protomaps.basemap.text.FontRegistry;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Basemap extends ForwardingProfile {

  public Basemap(NaturalEarthDb naturalEarthDb, QrankDb qrankDb) {

    var earth = new Earth();
    registerHandler(earth);

    //registerSourceHandler("osm", earth::processOsm);
    registerSourceHandler("osm_land", earth::processPreparedOsm);
    registerSourceHandler("ne", earth::processNe);
  }

  @Override
  public String name() {
    return "Protomaps Basemap";
  }

  @Override
  public String description() {
    return "Basemap layers derived from OpenStreetMap and Natural Earth";
  }

  @Override
  public String version() {
    return "4.0.4";
  }

  @Override
  public boolean isOverlay() {
    return false;
  }

  @Override
  public String attribution() {
    return """
      <a href="https://www.openstreetmap.org/copyright" target="_blank">&copy; OpenStreetMap</a>
      """.trim();
  }

  @Override
  public Map<String, String> extraArchiveMetadata() {
    Map<String, String> result = new HashMap<>();

    FontRegistry fontRegistry = FontRegistry.getInstance();
    List<String> scripts = fontRegistry.getScripts();

    for (String script : scripts) {
      result.put("pgf:" + script.toLowerCase() + ":name", fontRegistry.getName(script));
      result.put("pgf:" + script.toLowerCase() + ":version", fontRegistry.getVersion(script));
    }

    return result;
  }

  public static void main(String[] args) {
    run(Arguments.fromArgsOrConfigFile(args));
  }

  static void run(Arguments args) {
    // We explicitly set maxzoom to 13, since we dont go over zoom 13!
    args = args.orElse(Arguments.of("maxzoom", 13));

    Path dataDir = Path.of("data");
    Path sourcesDir = dataDir.resolve("sources");

    Path nePath = sourcesDir.resolve("natural_earth_vector.sqlite.zip");
    String neUrl = "https://naciscdn.org/naturalearth/packages/natural_earth_vector.sqlite.zip";

    String area = args.getString("area", "geofabrik area to download", "monaco");

    var planetiler = Planetiler.create(args)
      .addNaturalEarthSource("ne", nePath, neUrl)
      .addShapefileSource("osm_land", sourcesDir.resolve("land-polygons-split-3857.zip"),
        "https://osmdata.openstreetmap.de/download/land-polygons-split-3857.zip");

    Path pgfEncodingZip = sourcesDir.resolve("pgf-encoding.zip");
    Downloader.create(planetiler.config()).add("ne", neUrl, nePath)
      .add("pgf-encoding", "https://wipfli.github.io/pgf-encoding/pgf-encoding.zip", pgfEncodingZip)
      .run();
    //      .add("qrank", "https://qrank.wmcloud.org/download/qrank.csv.gz", sourcesDir.resolve("qrank.csv.gz")).run();

    var tmpDir = nePath.resolveSibling(nePath.getFileName() + "-unzipped");
    var naturalEarthDb = NaturalEarthDb.fromSqlite(nePath, tmpDir);
    //    var qrankDb = QrankDb.fromCsv(sourcesDir.resolve("qrank.csv.gz"));
    var qrankDb = QrankDb.empty();

    planetiler.setProfile(new Basemap(naturalEarthDb, qrankDb)).setOutput(Path.of(area + ".pmtiles"))
      .run();
  }
}
