![Logo](images/logo64.png)

# $application.title$

A text editor that uses [interpolated strings](https://en.wikipedia.org/wiki/String_interpolation) to reference externally defined values.

## Download

Download one of the following editions:

* [Windows](https://gitreleases.dev/gh/DaveJarvis/scrivenvar/latest/scrivenvar.exe)
* [Linux](https://gitreleases.dev/gh/DaveJarvis/scrivenvar/latest/scrivenvar.bin)
* [Java Archive](https://gitreleases.dev/gh/DaveJarvis/scrivenvar/latest/scrivenvar.jar)

## Run

Note that the first time the application runs, it will unpack itself into a local directory. Subsequent starts will be faster.

### Windows

On Windows, double-click the application to start. You will have to give the application permission to run.

### Linux

On Linux, run `chmod +x scrivenvar.bin` then `./scrivenvar.bin`.

## Features

* User-defined interpolated strings
* Real-time preview with variable substitution
* Auto-complete variable names based on variable values
* XML document transformation using XSLT3 or older
* Platform independent (Windows, Linux, MacOS)
* R integration

## Usage

See the following documents for more information:

* [USAGE.md](USAGE.md) -- Variable definitions and string interpolation
* [USAGE-R.md](USAGE-R.md) -- Call R functions within R Markdown documents
* [USAGE-SVG.md](USAGE-SVG.md) -- Fix known issues with displaying SVG files

## Future Features

* Spell check
* Search and replace using variables
* Re-organize variable names

## Screenshot

![Screenshot](images/screenshot.png)

## License

This software is licensed under the [BSD 2-Clause License](LICENSE.md).

