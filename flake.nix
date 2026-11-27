{
  description = "A Nix-flake-based Kotlin development environment";

  inputs = {
    #nixpkgs.url = "https://channels.nixos.org/nixos-26.05/nixexprs.tar.zst";
    nixpkgs.url = "https://channels.nixos.org/nixos-unstable/nixexprs.tar.zst";
  };

  outputs =
    { self, ... }@inputs:

    let
      javaVersion = 25; # Change this value to update the whole stack

      supportedSystems = [
        "x86_64-linux"
        "aarch64-linux"
        "aarch64-darwin"
      ];
      forEachSupportedSystem =
        f:
        inputs.nixpkgs.lib.genAttrs supportedSystems (
          system:
          f {
            inherit system;
            pkgs = import inputs.nixpkgs {
              inherit system;
              overlays = [ inputs.self.overlays.default ];
            };
          }
        );
    in
    {
      overlays.default =
        final: prev:
        let
          jdk = prev."jdk${toString javaVersion}";
        in
        {
          gradle = prev.gradle.override { java = jdk; };
          gradle_9 = prev.gradle_9.override { java = jdk; };
          kotlin = prev.kotlin.override { jre = jdk; };
        };

      devShells = forEachSupportedSystem (
        { pkgs, system }:
        let
          jdk = pkgs."jdk${toString javaVersion}";
          # Native libraries Compose Desktop (Skiko/AWT) needs on NixOS.
          # alsa-lib/libpulseaudio are deliberately NOT here: a store libasound on
          # LD_LIBRARY_PATH hides the system PipeWire ALSA plugin from Java Sound.
          desktopLibs = with pkgs; [
            libGL
            libx11
            libxext
            libxrender
            libxtst
            libxi
            libxrandr
            fontconfig
            freetype
          ];
        in
        {
          default = pkgs.mkShellNoCC {
            JAVA_HOME = jdk.home;
            LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath desktopLibs;
            packages = with pkgs; [
              jdk
              android-tools
              gcc
              gradle_9
              kotlin
              ncurses
              patchelf
              zlib
              self.formatter.${system}

              bun
            ];
          };
        }
      );

      formatter = forEachSupportedSystem ({ pkgs, ... }: pkgs.nixfmt);
    };
}
