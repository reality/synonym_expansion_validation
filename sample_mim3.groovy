@Grab('com.xlson.groovycsv:groovycsv:1.3')
@Grab(group='org.apache.commons', module='commons-lang3', version='3.4')
@Grab(group='edu.stanford.nlp', module='stanford-corenlp', version='3.7.0')
@Grab(group='edu.stanford.nlp', module='stanford-corenlp', version='3.7.0', classifier='models')
@Grab(group='edu.stanford.nlp', module='stanford-parser', version='3.7.0')

import static com.xlson.groovycsv.CsvParser.parseCsv
import edu.stanford.nlp.pipeline.*
import edu.stanford.nlp.ling.*
import edu.stanford.nlp.semgraph.*
import org.apache.commons.lang3.RandomUtils

println "Loading file... "

def entries = []
def file = new RandomAccessFile('./NOTEEVENTS.csv', 'r')

println "File loaded. Beginning selection sequence..."

while(entries.size() < 1000) {
  def rPos = RandomUtils.nextLong(new Long(0), file.length())

  file.seek(rPos)

  def foundNewRecord
  def newText
  
  while(!foundNewRecord) {
    newText = file.readLine()
    if(newText.indexOf('",') != -1) {
      foundNewRecord = true 
    }    
  }

  foundNewRecord = false
  def textRecord = ''

  while(!foundNewRecord) {
    newText = file.readLine() 
    if(newText.indexOf('",') != -1) {
      foundNewRecord = true 
    } else {
      textRecord += newText
    }    
  }

  if(textRecord.indexOf('     ') == -1) {
    entries << textRecord.replaceAll('\n', '').replaceAll('\\s+', ' ').replaceAll('\\.', '. ')
  }
    println entries.size()
}

println "Selecting sentences...."

def props = new Properties()
props.put("annotators", "tokenize, ssplit")
coreNLP = new StanfordCoreNLP(props)
pipeline = new AnnotationPipeline()
pipeline.addAnnotator(coreNLP)

def i = 0
def sentences = entries.collect { entry ->
  def aDocument = new Annotation(entry.toLowerCase())

  pipeline.annotate(aDocument)

  println "${++i}"

  def s = aDocument.get(CoreAnnotations.SentencesAnnotation.class).collect { it.toString() }
  s
}.flatten()
sentences.removeAll([null])

new File('mim3_entry_sample.txt').text = sentences.join('\n')
