import subprocess as sb

"""
Avendo come input il file con tutte le query,
eseguiamo il retrieve con SOLO e soltanto una query
e ed eval per solo e soltanto tale query.
"""
output_file = "data_out/NDCG.txt"
all_qrels_file = "data/qrels"
out_qrels_file = "data_out/qrels"
all_queries_file = "data/query-text.trec"
out_queries_file = "data_out/queries.trec"

# vector that contains NDCG_cut for BM25 and BM25P in the inferred position
out_v = []


def retrieve_eval():
    # execute the retrieve for BM25
    ka = []
    for arg in ["-Dtrec.model=BM25", ""]:
        sb.run(["bin/terrier", "batchretrieve", arg])
        # execute the eval
        proc = sb.Popen(
            ["bin/terrier", "trec_eval", out_qrels_file, "var/output/results.txt", "-m", "ndcg"],
            stdout=sb.PIPE)

        while True:
            line = proc.stdout.readline()
            if not line:
                break
            line = line.rstrip().decode("utf-8")

            if line.startswith('ndcg'):
                real_value = line.split("\tall\t")
                ka.append(real_value[1].strip())

    out_v.append(ka)


# reading the file with all queries
def write_q_rels_file(query_ID):
    with open(out_qrels_file, "w") as fpw:
        with open(all_qrels_file, "r") as fp:
            r = fp.readline()
            # it's only an optimization
            started = False
            while r != "":
                r_data = r.split(" ", 4)
                assert len(r_data) == 4

                if r_data[0] == query_ID:
                    started = True
                    fpw.write(r)
                else:
                    if started is True:
                        break
                r = fp.readline()


with open(all_queries_file, "r") as fp:
    line = fp.readline()

    while line != "":
        # writing the query-text file
        with open(out_queries_file, "w") as fpX:
            fpX.write(line)

        q_split = line.split(":")
        assert len(q_split) == 2

        # writing the qrels file
        write_q_rels_file(q_split[0])

        # calling the function that manages retrieve-eval function
        retrieve_eval()
        line = fp.readline()

with open(output_file, "w") as fo:
    for d in out_v:
        fo.write("%s,%s\n" % (d[0], d[1]))
