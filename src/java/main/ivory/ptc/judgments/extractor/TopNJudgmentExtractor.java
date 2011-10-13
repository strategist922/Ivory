/**
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

package ivory.ptc.judgments.extractor;

import ivory.ptc.data.AnchorTextTarget;
import ivory.ptc.data.PseudoJudgments;
import edu.umd.cloud9.io.array.ArrayListWritable;


/**
 * Extracts top N target documents and returns them as pseudo judgments.
 * Documents are chosen based on their relevence scores.
 *
 * The only parameter to this extractor is an integer N.
 *
 * @author Nima Asadi
 */
public class TopNJudgmentExtractor implements PseudoJudgmentExtractor {
	private static int n = 10;

  @Override
	public PseudoJudgments getPseudoJudgments(ArrayListWritable<AnchorTextTarget> anchorTextTargets) {
		PseudoJudgments judgments = new PseudoJudgments();
		int i = 0;
		for(AnchorTextTarget a :  anchorTextTargets) {
			if(i == n) {
				break;
			}
			judgments.add(a.getTarget(), a.getWeight());
			i++;
		}
		return judgments;
	}

  @Override
	public void setParameters(String[] params) {
		n = Integer.parseInt(params[0]);
	}
}
