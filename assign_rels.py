qrels = {}

with open("qrels.robust2004.txt", "r") as f:
  for line in f:
    qid, _, fid, rel = line.split(' ')
    qrels[(qid, fid)] = int(rel)

with open("matched_docs.txt") as f:
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

