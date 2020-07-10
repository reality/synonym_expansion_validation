@Grab(group='org.semanticweb.elk', module='elk-owlapi', version='0.4.3')
@Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='4.2.5')
@Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='4.2.5')
@Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='4.2.5')
@Grab(group='com.github.sharispe', module='slib-sml', version='0.9.1')

import groovyx.gpars.*
import org.codehaus.gpars.*
import java.util.concurrent.*

import org.openrdf.model.URI;
import slib.graph.algo.accessor.GraphAccessor;
import slib.graph.algo.utils.GAction;
import slib.graph.algo.utils.GActionType;
import slib.graph.algo.validator.dag.ValidatorDAG;
import slib.graph.io.conf.GDataConf;
import slib.graph.io.conf.GraphConf;
import slib.graph.io.loader.GraphLoaderGeneric;
import slib.graph.io.util.GFormat;
import slib.graph.model.graph.G;
import slib.graph.model.impl.graph.memory.GraphMemory;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.repo.URIFactory;
import slib.sml.sm.core.engine.SM_Engine;
import slib.sml.sm.core.metrics.ic.utils.IC_Conf_Topo;
import slib.sml.sm.core.metrics.ic.utils.ICconf;

import slib.sml.sm.core.metrics.ic.utils.IC_Conf_Corpus;
import slib.sml.sm.core.utils.SMConstants;
import slib.sml.sm.core.utils.SMconf;
import slib.utils.ex.SLIB_Exception;
import slib.utils.impl.Timer;

def aFile = args[0]
def rmneg = args[1] == 'true'

def patient_visit_diagnoses = [:]


new File('./sampled_patient_visits.csv').splitEachLine('\t') {
  patient_visit_diagnoses[it[0]] = it[1].tokenize(',').collect { o -> 
  
    o = o.replaceAll('"','')
    o
  }
}

ConcurrentHashMap aMap = [:]

def cList = []

def aFileContent = []
new File(aFile).splitEachLine('\t') { aFileContent << it }

def t = 0
aFileContent.each {
  if(it[0] && it[1]) {
  it[0] = it[0].tokenize('.')[0]
  if(patient_visit_diagnoses.containsKey(it[0])) {
    if(rmneg && it[3] && it[3].indexOf('neg') != -1 ) { return ;}
    if(!aMap.containsKey(it[0])) {
      aMap[it[0]] = []
    }
    it[1] = it[1].replace('<', '').replace('>', '')
    it[1] = 'DOID:' + it[1].tokenize('_').last()

    if(!aMap[it[0]].contains(it[1])) {
      aMap[it[0]] << it[1]
    }

    cList << it[1]
    println "${++t}/${aFileContent.size()}"
    }
  }
}

println 'writing the annotation file now'
def sWriter = new BufferedWriter(new FileWriter('annot.tsv'))
def oo = ''
def y = 0
aMap.each { a, b ->
  println "(${++y}/${aMap.size()})"
  if(!b.any{ it.indexOf('txt') != -1}) {
    sWriter.write('http://reality.rehab/ptvis/' + a + '\t' + b.join(';') + '\n')
  }
}
sWriter.flush()
sWriter.close()
println 'done'

cList = cList.unique()

println cList

def ontoFile = './doid.owl'

def factory = URIFactoryMemory.getSingleton()
def graphURI = factory.getURI('http://doid/')
factory.loadNamespacePrefix("DOID", graphURI.toString());

def g = new GraphMemory(graphURI)

def dataConf = new GDataConf(GFormat.RDF_XML, ontoFile)
def actionRerootConf = new GAction(GActionType.REROOTING)
//actionRerootConf.addParameter("root_uri", "HP:0000118"); // phenotypic abnormality
actionRerootConf.addParameter("root_uri", "DOID:4"); // phenotypic abnormality

def gConf = new GraphConf()
gConf.addGDataConf(dataConf)
gConf.addGAction(actionRerootConf)

def annot = 'annot.tsv'
gConf.addGDataConf(new GDataConf(GFormat.TSV_ANNOT, annot));

GraphLoaderGeneric.load(gConf, g)

println g.toString()

def roots = new ValidatorDAG().getTaxonomicRoots(g)
println roots

def icConf = new IC_Conf_Corpus(SMConstants.FLAG_IC_ANNOT_RESNIK_1995)
//def icConf = new IC_Conf_Topo(SMConstants.FLAG_ICI_ZHOU_2008)
//def icConf = new IC_Conf_Topo(SMConstants.FLAG_ICI_SANCHEZ_2011)
def smConfPairwise = new SMconf(SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_RESNIK_1995, icConf)
//def smConfPairwise = new SMconf(SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_LIN_1998, icConf)
//def smConfGroupwise = new SMconf(SMConstants.FLAG_SIM_GROUPWISE_AVERAGE, icConf)
def smConfGroupwise = new SMconf(SMConstants.FLAG_SIM_GROUPWISE_BMA, icConf)
// FLAG_SIM_GROUPWISE_AVERAGE_NORMALIZED_GOSIM

//def smConfPairwise = new SMconf(SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_JIANG_CONRATH_1997_NORM , icConf)


def out = []
def z = 0

def outWriter = new BufferedWriter(new FileWriter(aFile + '_sim_matrix_fast5.lst'), 1024 * 1024 * 1024)
if(rmneg) {
outWriter = new BufferedWriter(new FileWriter(aFile + '_sim_matrix_fast5_noneg.lst'), 1024 * 1024 * 1024)
}

def engine = new SM_Engine(g)

cList = cList.unique()


def rrs=[]
def aps=[]
aMap.each { g1, u1 ->
  println "(${++z}/${aMap.size()})"

  def aList = []
  aMap.each { g2, u2 ->
    def match = patient_visit_diagnoses[g1][0] == patient_visit_diagnoses[g2][0]
    def match2 = patient_visit_diagnoses[g1].any { v -> patient_visit_diagnoses[g2].contains(v) }
    def match3 = patient_visit_diagnoses[g2].any { patient_visit_diagnoses[g1][0] == it }

    aList << [g1,g2,engine.compare(smConfGroupwise, smConfPairwise,
                                    u1.collect { factory.getURI('http://purl.obolibrary.org/obo/'+it.replace(':','_')) }.toSet(), 
                                    u2.collect { factory.getURI('http://purl.obolibrary.org/obo/'+it.replace(':','_')) }.toSet())
                                    
                                   // u1.collect { factory.getURI(it) }.toSet(), 
                                    //u2.collect { factory.getURI(it) }.toSet())
                                    ,match,match2,match3]
  }
  aList = aList.toSorted { it[2] }.reverse() //[0..10]

  def matchRank = 0
  aList.eachWithIndex { it, i ->
    if(matchRank > 0) { return; }
    if(patient_visit_diagnoses[it[0]][0] == patient_visit_diagnoses[it[1]][0]) {
      matchRank = i+1 
    }
  }
  rrs << 1/matchRank //

  aList = aList[0..10]

  def ps = []  // precisions
  // for each patient it[1] similar to it[0], see if it[1] contains the diagnosis from it[0] in position 0
  aList.eachWithIndex { it, i ->
    if(patient_visit_diagnoses[it[1]][0] == patient_visit_diagnoses[it[0]][0]) {
      ps << (ps.size()+1) / (i+1) //
    }
  }
  def ap
  try {
    ap = ps.sum() / ps.size() //
  } catch(e) { ap = 0 }
  println ap
  aps << ap
  
  aList.eachWithIndex { it, i -> 
    outWriter.write(it.join(',') + ',' + (i+1) + '\n')
  }
}

def mrr = rrs.sum() / rrs.size() //
def map = aps.sum() / aps.size() //
println 'mrr: ' + mrr
println 'map: ' + map


/*
self-implementation of BMA (not used in experiment, but it can be faster for larger amounts of patients)
def cSim = [:] // class similarity
cList.eachWithIndex { u1, i ->
  println "${i}/${cList.size()}"
  cSim[u1] = [:]
  cList.each { u2 ->
    cSim[u1][u2] = engine.compare(smConfPairwise, 
      factory.getURI('http://purl.obolibrary.org/obo/'+u1.replace(':','_')),
      factory.getURI('http://purl.obolibrary.org/obo/'+u2.replace(':','_')))
  }
}

println 'doing bma'

GParsPool.withPool(45) { p ->  // im a monster
aMap.eachParallel { g1, u1 ->
  println "(${++z}/${aMap.size()})"
  def aList = []
  aMap.each { g2, u2 ->
    aList << [
      g2,
      ((u1.inject(0) { sum, uri ->
        sum += u2.collect { uri2 -> cSim[uri][uri2] }.max()
      } + u2.inject(0) { sum, uri ->
        sum += u1.collect { uri2 -> cSim[uri][uri2] }.max()
      }) / (u1.size() + u2.size())) // /
    ]
  }

  aList.toSorted { it[1] }.reverse().eachWithIndex { i, it ->
    outWriter.write(g1 + ',' + it[0] + ',' + (i+1) + ','+ it[1]+ ',' + match + ','+match2+'\n')
  }
}
}*/

outWriter.flush()
outWriter.close()

//new File(aFile + '_sim_matrix.lst').text = out.join('\n')

//sum += u2.inject(0) { max, uri2 -> (cSim[uri][uri2] > max) ? cSim[uri][uri2] : max }
//sum += u1.inject(0) { max, uri2 -> (cSim[uri][uri2] > max) ? cSim[uri][uri2] : max }
