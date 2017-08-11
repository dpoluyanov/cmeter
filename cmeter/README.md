## CMeter
Faster, than u think.

Around 107000000 measures per second on MBP at ~35ns overhead per every measure storing

And 52000000 measures per second on same MBP at ~19ns latency per every measure store in single-core mode

`J8_Store`, that verbose meters uses by default can be more efficient in concurrent environment with `-XX:-RestrictContended` JVM flag
