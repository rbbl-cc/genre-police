let

  nixpkgs = fetchTarball "https://github.com/NixOS/nixpkgs/tarball/nixos-24.05";

  pkgs = import nixpkgs { config = {}; overlays = []; };
  gradlepkgs = import (fetchTarball "https://github.com/NixOS/nixpkgs/archive/68bb040a9617ec704cb453cc921f7516d5b36cae.tar.gz") {};
in


pkgs.mkShellNoCC {

  packages = with pkgs; [
    temurin-bin-17
    # gradlepkgs.gradle
    gradle
  ];

}
