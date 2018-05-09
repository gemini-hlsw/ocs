
# edu.gemini.oodb.too.url

This bundle originated from `edu.gemini.oodb.too.url` in the OCS 1.5 build.


## HTTP TOO Protocol

### Parameters

#### Basic Parameters

| Parameter  | Format/Example          | Reqd?  | Notes
|------------|-------------------------|:------:|-------
| `posangle` | 123.45                  |✓       | position angle in degrees
| `note`     | `hurry!`                |✓       | text of the note to add to the cloned template observation
| `group`    | `too-group`             |        | optional name of destination group for cloned observation
| `ready`    | `true` or `false`       |✓       | whether to mark the cloned template observation as ready
| `prog`     |  `GS-2006A-Q-23`        |✓       | program id
| `email`    | `bob@burger.com`        |✓       | user key
| `password` | `banana123`             |✓       | user password
| `obs`      | `K twilight X`          |1       | name of the template observation to update
| `obsnum`   | `5`                     |1       | `number` of the template observation to update

1 - One of `obs` and `obsnum` must be supplied.

#### Base Position Details

| Parameter | Format/Example          | Reqd?  | Notes
|-----------|-------------------------|:------:|-------
| `target`  | `ABC12-X1`              |✓       | base position name
| `ra`      | `12.3` or `12:34:56.78` |✓       | base position right ascension, degrees or DMS
| `dec`     | `12.3` or `01:34:56.78` |✓       | base position declination, degrees or HMS
| `mags`    | `1.2/U/Vega`            |2       | base position magnitudes, comma separated

2 - Magnitudes are optional, but when supplied must contain all three elements (value, band, system). Multiple magnitudes can be supplied; use a comma to delimit them (for example `1.2/U/Vega,3.4/R/AB`). Magnitudes can be specified in `Vega`, `AB` or `Jy` systems in the following bands:

| Band |Central Wavelength | Description
|------|------------------:|--------------
| `U`  |            365 nm | ultraviolet
| `B`  |            445 nm | blue
| `V`  |            551 nm | visual
| `UC` |            610 nm | UCAC
| `R`  |            658 nm | red
| `I`  |            806 nm | infrared
| `Y`  |           1020 nm |
| `J`  |           1220 nm |
| `H`  |           1630 nm |
| `K`  |           2190 nm |
| `L`  |           3450 nm |
| `M`  |           4750 nm |
| `N`  |          10000 nm |
| `Q`  |          16000 nm |
| `AP` |                -- | apparent

Supported magnitude systems.


#### Guide Star Details

Guide stars are optional, but if any of the parameters are specified then all
required parameters must be specified.

| Parameter | Format/Example            | Reqd?  | Notes
|-----------|---------------------------|:------:|-------
| `gstarget`| `ABC12-X1               |        | guide star name (defaults to `GS` if missing)
| `gsra`    | `12.3` or `12:34:56.78` |✓       | guide star right ascension, degrees or DMS
| `gsdec`   | `12.3` or `01:34:56.78` |✓       | guide star declination, degrees or HMS
| `gsmags`  | `1.2/U/Vega`            |        | guide star magnitudes, comma separated
| `gsprobe` | `PWFS1`                 |✓       | guide probe

Allowed guide probe values are `PWFS1`, `PWFS2`, `OIWFS`, and `AOWFS`.

#### Example Queries

* With guide star
```
curl -k "https://localhost:8443/too?prog=GS-2018A-Q-1&posangle=0.0&note=hello&ready=true&email=bob@burger.com&password=password&obsnum=4&target=canopus&ra=06:23:57.110&dec=-52:41:44.38&gstarget=187-007980&gsra=06:23:39.584&gsdec=-52:41:16.43&gsprobe=OIWFS"
```

* Without guide star
```
curl -k "https://localhost:8443/too?prog=GS-2018A-Q-1&posangle=0.0&note=hello&ready=true&email=bob@burger.com&password=password&obsnum=4&target=canopus&ra=06:23:57.110&dec=-52:41:44.38"
```








