qrels = {}
import sys
assert len(sys.argv) == 3
qrels_file = sys.argv[2]
docs_file  = sys.argv[1]
with open(qrels_file, "r") as f:
  for line in f:
    qid, _, fid, rel = line.split(' ')
    qrels[(qid, fid)] = int(rel)

with open(docs_file, "r") as f:
  for line in f:
    terms = line.split(' ')
    qid, fid = terms[0], terms[1]
    rel = 0
    if (qid, fid) in qrels: rel = qrels[(qid, fid)]
    print rel,
    print "qid:" + qid,
    for term in terms[2:]:
      print term.strip(),
    print "#" + fid

