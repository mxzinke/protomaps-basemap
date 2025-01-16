## Docker

> Notice: This version of the basemap has been limited to not use osm raw data and is limited to max z13!

```
docker build -t protomaps/basemaps .
```

```
docker run -v ./data:/tiles/data --rm -it protomaps/basemaps --output=data/earth.pmtiles --area=earth --download
```

