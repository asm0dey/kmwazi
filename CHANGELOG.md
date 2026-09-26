# Changelog

All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

## 2.0.2

- A result needs at least two fingers on the screen; a single finger no longer wins by itself.
- Colorblind and Lucid palettes no longer contain near-black colours that vanish on the dark background.

## 2.0.1

- A new finger takes a colour no other finger on the screen is using; colours of lifted fingers are reused.

## 2.0.0

- Rewritten from scratch on Compose Multiplatform; same features and settings.
- Circles are sized in dp, so they look the same on every screen density.
- Every finger is drawn (previously only the first 10).
- Groups show group numbers; Order and Groups labels pick black or white for readable contrast.
- The result overlay fades out faster.
- Group size is 2–10 everywhere.
