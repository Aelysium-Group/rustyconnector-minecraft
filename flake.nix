{
  description = "RustyConnector dev environment";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs =
    { self, nixpkgs }:
    let
      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
    in
    {
      devShells = forAllSystems (
        system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
        in
        {
          default = pkgs.mkShell {
            packages = with pkgs; [
              jdk21
              # NO gradle here on purpose: the wrapper (./gradlew) pins and
              # self-provisions Gradle 9.6.0 (Loom 1.17 requires Gradle 9).
              # nixpkgs' gradle_9 lags (9.4.1 at last check) — shipping a second,
              # older Gradle in the shell is a drift trap, not a convenience.
              maven
              just
            ];
            JAVA_HOME = pkgs.jdk21.home;
          };
        }
      );
    };
}
