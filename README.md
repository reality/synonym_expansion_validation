A validation of the synonym expansion experiment, as described in the paper, can
be examined and performed using the files in this repository. You will need to
install Komenti to run the validation yourself, but all generated files should
be here. If you run the commands yourself, the results may differ, since the
ontologies on AberOWL will likely have been updated since the commands were
originally run.

## Expanded HPO

HPO synonyms generated are vailable in the files *hpo/all_unexpanded.txt* and
*hpo/all_expanded.txt*. You can generate them yourself with the following
Komenti commands:

```bash
./Komenti query
```

## Manual validation

You can randomly select a number of entries from the HPO expansion, that do not
include the labels already included in HPO, by running the Groovy script
*generate_random_sample.groovy*. They will be printed to stdout, and you can 
save them to a file - for example, *validation.csv*. These will have to be 
manually validated, by you or a friend. However, the selection of labels we 
used to validate, along with their validations, are available in
*manual_validation.tsv*.

## Cardiovascular Annotation Evaluation

You will have to obtain the MIMIC-III files yourself. But when you do, you can
access the NOTEEVENTS.csv file. Then, you can use *sample_mim3.groovy* to
extract random entries from it. It's a bit interesting to read random samples
from such a large file.

Then, you can generate labels files for cardiovascular abnormalities. These are
also stored in *cardio/* and *cardio/*.

```bash
./Komenti query -cl 'Abnormality of the cardiovascular system' -o HP --out cardio/unexpanded_cardiovasc.txt
./Komenti query -cl 'Abnormality of the cardiovascular system' -o HP --expand-synonyms --out cardio/expanded_cardiovasc.txt
```

Then, we just have to annotate the output. These files are stored in *cardio/unexp_ann.tsv* and *cardio/exp_ann.tsv*.

```bash

```


## MEDLINE evaluation

