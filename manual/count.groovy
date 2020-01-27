def tp = 0
def fp = 0
def amb = 0

new File('marked_validation.tsv').splitEachLine('\t') { f ->
  switch(f[3]) {
    case '1': tp++; break;
    case '2': fp++; break;
    default: amb++
  }
}

def precision = tp / (tp+fp)
println "TP: $tp"
println "FP: $fp"
println "Precision: $precision"
println "Ambiguous: $amb"
