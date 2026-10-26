# PosixShell-J

A POSIX-compliant Unix shell built from scratch in Java, featuring a custom parser with quote/escape handling, builtins (`cd`, `pwd`, `echo`, `type`), `$PATH` executable resolution, I/O redirection, multi-command pipelines, background job control, tab autocompletion, and persistent command history.

## Features

- REPL loop reading and executing commands interactively
- Hand-written tokenizer/parser handling single quotes, double quotes, and backslash escaping
- Builtins: `cd` (absolute, relative, and `~` home-directory paths), `pwd`, `echo`, `type`, `exit`
- `$PATH`-based executable resolution and process spawning via `ProcessBuilder`
- I/O redirection: `>`, `>>` (stdout), `2>`, `2>>` (stderr)
- Tab-based autocompletion
- Multi-command pipelines and background job control *(in progress)*

## Build & Run

```sh
./run.sh
```

This compiles the project with Maven and runs the resulting jar.

## Tech

Java, `ProcessBuilder`, `java.nio.file`, raw terminal I/O
