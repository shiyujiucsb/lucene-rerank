K = 20

import warnings
import numpy as np
from sklearn.metrics import roc_auc_score
from scipy.stats import spearmanr
from math import log
import sys
assert len(sys.argv) == 2

feature_lists  = {}
relevance_list = []
raw_relevance_list = []
qid_lists = {}

def P_at_k(ranking_list, rel_list, K):
  precision = 0.0
  for qid in qid_lists:
    top_docs = sorted([(ranking_list[i], rel_list[i]) for i in qid_lists[qid]], \
               key = lambda x: x[0], reverse=True)[:K]
    precision += sum([x[1] for x in top_docs]) * 1.0 / K
  return precision / len(qid_lists)

def NDCG_at_k(ranking_list, rel_list, K):
  ndcg = 0.0
  n_queries = 0
  for qid in qid_lists:
    top_docs = sorted([(ranking_list[i], rel_list[i]) for i in qid_lists[qid]], \
               key = lambda x: x[0], reverse=True)[:K]
    true_top_rels = sorted([rel_list[i] for i in qid_lists[qid]], reverse=True)[:K]
    assert len(top_docs) == len(true_top_rels)
    if len(top_docs) == 0: continue
    if true_top_rels[0] == 0: continue
    dcg  = sum((2**top_docs[i][1]-1) * log(2.0) / log(i+2) for i in range(len(top_docs)))
    idcg = sum((2**true_top_rels[i]-1) * log(2.0) / log(i+2) for i in range(len(true_top_rels)))
    ndcg += dcg/idcg
    n_queries += 1
  if n_queries == 0: return 0
  return ndcg / n_queries

def avg_AUC(rel_list, ranking_list):
  auc = 0.0
  n_queries = 0
  for qid in qid_lists:
    if len(qid_lists[qid]) < 100: continue
    if len(set([rel_list[i] for i in qid_lists[qid]])) < 2: continue
    n_queries += 1 
    auc += roc_auc_score([rel_list[i] for i in qid_lists[qid]], \
                          [ranking_list[i] for i in qid_lists[qid]])
  if n_queries == 0: return 0
  return auc / n_queries

def avg_spearmanr(rel_list, ranking_list):
  r = 0.0
  n_queries = 0
  for qid in qid_lists:
    if len(qid_lists[qid]) < 100: continue
    if len(set([rel_list[i] for i in qid_lists[qid]])) < 2: continue
    if len(set([ranking_list[i] for i in qid_lists[qid]])) < 2: continue
    Y = [rel_list[i] for i in qid_lists[qid]]
    X = [ranking_list[i] for i in qid_lists[qid]]
    res = spearmanr(Y, X)
    r += res[0]
    n_queries += 1
  if n_queries == 0: return 0
  return r / n_queries

with open(sys.argv[1], "r") as f:
  for line in f:
    terms = line.split(' ')
    assert len(terms) > 3
    if int(terms[0]) > 0: 
      relevance_list.append(1)
    else:
      relevance_list.append(0)
    raw_relevance_list.append(int(terms[0]))
    assert ':' in terms[1]
    qid = int(terms[1].split(':')[1])
    if qid not in qid_lists:
      qid_lists[qid] = []
    qid_lists[qid].append(len(relevance_list)-1)
    for term in terms[2:]:
      if ':' not in term: break
      fid, val = term.split(':')
      fid, val = int(fid), float(val)
      if fid not in feature_lists:
        feature_lists[fid] = []
      feature_lists[fid].append(val)

for feat in sorted(feature_lists.keys()):
  AUC = avg_AUC(relevance_list, feature_lists[feat])
  r = avg_spearmanr(raw_relevance_list, feature_lists[feat])
  p = P_at_k(feature_lists[feat], relevance_list, K)
  ndcg = NDCG_at_k(feature_lists[feat], raw_relevance_list, K)
  print "{0:3d} {1:1.3f} {2:1.3f} {3:1.3f} {4:1.3f}".format( \
         feat, p, ndcg, AUC, r)

