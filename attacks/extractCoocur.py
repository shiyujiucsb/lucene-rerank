# Usage:
# python extractCooccur.py querySetFile postingFile

import sys
assert len(sys.argv) == 3
querySetFile, postingFile = sys.argv[1], sys.argv[2]

postings = {}

with open(postingFile, "r") as lines:
  for line in lines:
    term = line.strip().split(" ")[0]
    postings[term] = set()
    for docid in line.strip().split(" ")[1:]:
      postings[term].add(docid)

xterms = {}

def normalize(s):
  spaced = True
  res = ""
  for c in s.strip():
    if ord('a') <= ord(c) <= ord('z'):
      res += c
      spaced = False
    elif ord('A') <= ord(c) <= ord('Z'):
      res += c.lower()
      spaced = False
    elif ord('0') <= ord(c) <= ord('9'):
      res += c
      spaced = False
    else:
      if not spaced:
        res += " "
        spaced = True
  return res.strip()

with open(querySetFile, "r") as lines:
  for line in lines:
    terms = normalize(line).split(" ")[1:]
    assert len(terms) > 0
    sterm = terms[0]
    for term in terms[1:]:
      if sterm in postings: sterm_postlen = len(postings[sterm])
      else: sterm_postlen = 0
      if term in postings: term_postlen = len(postings[term])
      else: term_postlen = 0
      if term_postlen < sterm_postlen:
        sterm = term
    if sterm not in xterms:
      xterms[sterm] = set()
    for term in terms:
      if term is not sterm:
        xterms[sterm].add(term);

sterms = xterms.keys()
n = len(sterms)
assert n > 1
for i in range(n-1):
  for j in range(i+1, n):
    if len(xterms[sterms[i]].intersection(xterms[sterms[j]])) > 0:
      if sterms[i] not in postings or sterms[j] not in postings: continue
      if len(postings[sterms[i]].intersection(postings[sterms[j]])) == 0: continue
      print sterms[i], sterms[j],
      for docid in postings[sterms[i]].intersection(postings[sterms[j]]):
        print docid,
      print ""

