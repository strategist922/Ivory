/*
 * Ivory: A Hadoop toolkit for Web-scale information retrieval
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package ivory.demo;

import ivory.collection.trec.BuildTrecForwardIndex;
import ivory.index.BuildInvertedIndexDocSorted;
import ivory.index.BuildPostingsForwardIndex;
import ivory.index.ComputeDictionarySize;
import ivory.index.MergePostings;
import ivory.util.RetrievalEnvironment;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

import edu.umd.cloud9.collection.trec.NumberTrecDocuments;

/**
 * <p>
 * Main driver program for indexing TREC collections. This class is currently
 * hard-coded to index TREC disks 4-5 (minus FR and CR), but can be adapted to
 * index any TREC-formatted collection with minimal changes.
 * </p>
 * 
 * <p>
 * The program takes four command-line arguments:
 * </p>
 * 
 * <ul>
 * <li>[input-path] path to the document collection</li>
 * <li>[index-path] path to index directory</li>
 * <li>[num-mappers] number of mappers to run</li>
 * <li>[num-reducers] number of reducers to run</li>
 * </ul>
 * 
 * <p>
 * Here's a sample invocation:
 * </p>
 * 
 * <blockquote>
 * 
 * <pre>
 *  hadoop jar ivory.jar ivory.demo.DriverBuildIndexTrec \
 *  /umd/collections/trec/trec4-5_noCRFR.xml /umd/indexes/trec45noCRFR.positional/ 100 10
 * </pre>
 * 
 * </blockquote>
 * 
 * <p>
 * Running with 100 mappers and 10 reducers might be typical setting.
 * </p>
 * 
 * @author Jimmy Lin
 * 
 */
public class DriverBuildIndexTrec extends Configured implements Tool {
	private static final Logger sLogger = Logger.getLogger(DriverBuildIndexTrec.class);

	private DriverBuildIndexTrec() {
	}

	private static int printUsage() {
		System.out.println("usage: [input-path] [index-path] [num-mappers] [num-reduers]");
		ToolRunner.printGenericCommandUsage(System.out);
		return -1;
	}

	/**
	 * Runs this tool.
	 */
	public int run(String[] args) throws Exception {
		if (args.length != 4) {
			printUsage();
			return -1;
		}

		String collection = args[0];
		String root = args[1];
		int numMappers = Integer.parseInt(args[2]);
		int numReducers = Integer.parseInt(args[3]);

		sLogger.info("Tool: DriverBuildIndexTrec");
		sLogger.info(" - Collection path: " + collection);
		sLogger.info(" - Index path: " + root);
		sLogger.info(" - Number of mappers: " + numMappers);
		sLogger.info(" - Number of reducers: " + numReducers);

		Configuration conf = getConf();
		FileSystem fs = FileSystem.get(conf);

		// if index directory doesn't exist, create it...
		Path p = new Path(root);
		if (!fs.exists(p)) {
			sLogger.info("Index path doesn't exist, creating...");
			fs.mkdirs(p);
		}

		// if docid (String) -> docno (sequentially-number integer) mapping
		// doesn't exist, create it...
		String mappingFile = RetrievalEnvironment.getDocnoMappingFile(root);
		String mappingDir = RetrievalEnvironment.getDocnoMappingDirectory(root);

		p = new Path(mappingFile);
		if (!fs.exists(p)) {
			sLogger.info("docno-mapping.dat doesn't exist, creating...");
			String[] arr = new String[] { collection, mappingDir, mappingFile,
					new Integer(numMappers).toString() };
			NumberTrecDocuments tool = new NumberTrecDocuments();
			tool.setConf(conf);
			tool.run(arr);

			fs.delete(new Path(mappingDir), true);
		}

		// now we're getting ready to build the actual inverted index
		conf.setInt("Ivory.NumMapTasks", numMappers);
		conf.setInt("Ivory.NumReduceTasks", numReducers);

		conf.set("Ivory.CollectionName", "TREC_vol45");
		conf.set("Ivory.CollectionPath", collection);
		conf.set("Ivory.IndexPath", root);
		conf.set("Ivory.InputFormat", "edu.umd.cloud9.collection.trec.TrecDocumentInputFormat");
		conf.set("Ivory.DocnoMappingClass", "edu.umd.cloud9.collection.trec.TrecDocnoMapping");

		// The two choices for tokenizers are "ivory.util.GalagoTokenizer" or
		// "ivory.util.LuceneTokenizer"; the Galago tokenizer is moderately more
		// effective.
		conf.set("Ivory.Tokenizer", "ivory.util.GalagoTokenizer");

		// number of documents for trec45noCR: 528155, for trec45noCRFR: 472525
		conf.setInt("Ivory.CollectionDocumentCount", 472525);
		// docnos start at 1
		conf.setInt("Ivory.DocnoOffset", 0);
		// build positional index
		conf.setBoolean("Ivory.Positional", true);

		// task for building the inverted index
		BuildInvertedIndexDocSorted indexTool = new BuildInvertedIndexDocSorted(conf);
		// build the inverted index
		indexTool.run();

		// merge the postings into one large SequenceFile (optional)
		MergePostings mergeTool = new MergePostings(conf);
		mergeTool.run();

		// In order to build the terms to postings forward index, we need to
		// know the number of terms to include. ComputeDictionarySize does this
		// for us, controlled by the "Ivory.DfThreshold" parameter. A setting of
		// n means that we retain only terms with df > n. Therefore, a threshold
		// of 0 means we include all terms.

		int dfThreshold = 0;

		conf.setInt("Ivory.DfThreshold", dfThreshold);

		ComputeDictionarySize dictTool = new ComputeDictionarySize(conf);
		int n = dictTool.run();

		sLogger.info("Building terms to postings forward index with " + n + " terms");

		conf.setInt("Ivory.IndexNumberOfTerms", n);
		conf.setInt("Ivory.ForwardIndexWindow", 8);
		conf.setInt("Ivory.DfThreshold", 0);

		// build the actual terms to postings forward index
		BuildPostingsForwardIndex postingsTool = new BuildPostingsForwardIndex(conf);
		postingsTool.run();

		// build the document forward index
		BuildTrecForwardIndex findexTool = new BuildTrecForwardIndex(conf);
		findexTool.run();

		return 0;
	}

	/**
	 * Dispatches command-line arguments to the tool via the
	 * <code>ToolRunner</code>.
	 */
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new DriverBuildIndexTrec(), args);
		System.exit(res);
	}
}