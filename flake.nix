{
  description = "俺が払うよ";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/release-23.11";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils, ... }:
    {
      overlays.default = final: prev: rec {
        jre = prev.jdk21;
        sbt = prev.sbt.override {
          inherit jre;
        };
        scala_3 = prev.scala_3.override {
          inherit jre;
        };
        metals = prev.metals.override {
          inherit jre;
        };
      };
    } // flake-utils.lib.eachDefaultSystem (system:
      let pkgs = import nixpkgs { inherit system; };
      in {
        devShells.default = pkgs.mkShell {
          packages = with pkgs; [ jdk21 scala_3 sbt metals ];
        };
      });
}
