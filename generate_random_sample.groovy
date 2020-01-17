def initial = [:]
def expanded = [:]

new File('./hpo/unexpanded_all.txt').splitEachLine('\t') {
  if(!initial.containsKey(it[1])) {
    initial[it[1]] = []   
  }
  initial[it[1]] << it[0]
}
new File('./hpo/expanded_all.txt').splitEachLine('\t') {
  if(!expanded.containsKey(it[1])) {
    expanded[it[1]] = []   
  }
  if(!initial[it[1]].contains(it[0])) {
    expanded[it[1]] << it[0]
  }
}

def keys = expanded.keySet().toArray()

def r = new Random()
def found = 0

while(found < 500) {
  def i = keys[r.nextInt(keys.size())]
  expanded[i].each { z ->
    println "$i\t${initial[i][0]}\t${z}"
  }
  found++
}
