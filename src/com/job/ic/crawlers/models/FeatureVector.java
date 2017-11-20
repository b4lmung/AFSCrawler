/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.models;

public class FeatureVector {

	public static final String[] featureIndex = new String[] { "domain", "ip",
			"in-degree-Th", "in-degree-NonTh", "src-avg-percentTh",
			"src-domain-com-th", "src-domain-th-th", "src-domain-net-th",
			"src-domain-org-th", "src-domain-info-th", "src-domain-other-th",
			"src-domain-com-non", "src-domain-th-non", "src-domain-net-non",
			"src-domain-org-non", "src-domain-info-non",
			"src-domain-other-non", "src-ip-th", "src-ip-us", "src-ip-uk",
			"src-ip-jp", "src-ip-cn", "src-ip-other", "src-percentTh-in1",
			"src-percentTh-in2", "src-percentTh-in3", "src-percentTh-in4",
			"src-percentTh-in5", "class" };
	private String hostname;
	private String trgDomain;
	private String trgIpLocation;
	private int trgInDegreeTh;
	private int trgInDegreeNonTh;
	private int srcIpLocationTh;
	private int srcIpLocationUs;
	private int srcIpLocationUk;
	private int srcIpLocationJp;
	private int srcIpLocationCn;
	private int srcIpLocationOther;
	private double[] srcPercentTh;
	private double srcAvgPercentTh;
	private int srcThDomainCOM;
	private int srcThDomainTH;
	private int srcThDomainNET;
	private int srcThDomainORG;
	private int srcThDomainINFO;
	private int srcThDomainOTHER;
	private int srcNonDomainCOM;
	private int srcNonDomainTH;
	private int srcNonDomainNET;
	private int srcNonDomainORG;
	private int srcNonDomainINFO;
	private int srcNonDomainOTHER;

	public String getTrgDomain() {
		return trgDomain;
	}

	public void setTrgDomain(String trgDomain) {
		this.trgDomain = trgDomain;
	}

	public String getTrgIpLocation() {
		return this.trgIpLocation;
	}

	public void setTrgIpLocation(String trgIpLocation) {
		this.trgIpLocation = trgIpLocation;
	}

	public int getTrgInDegreeTh() {
		return this.trgInDegreeTh;
	}

	public void setTrgInDegreeTh(int trgInDegreeTh) {
		this.trgInDegreeTh = trgInDegreeTh;
	}

	public int getSrcIpLocationTh() {
		return srcIpLocationTh;
	}

	public void setSrcIpLocationTh(int srcIpLocationTh) {
		this.srcIpLocationTh = srcIpLocationTh;
	}

	public int getSrcIpLocationUs() {
		return srcIpLocationUs;
	}

	public void setSrcIpLocationUs(int srcIpLocationUs) {
		this.srcIpLocationUs = srcIpLocationUs;
	}

	public int getSrcIpLocationUk() {
		return srcIpLocationUk;
	}

	public void setSrcIpLocationUk(int srcIpLocationUk) {
		this.srcIpLocationUk = srcIpLocationUk;
	}

	public int getSrcIpLocationJp() {
		return srcIpLocationJp;
	}

	public void setSrcIpLocationJp(int srcIpLocationJp) {
		this.srcIpLocationJp = srcIpLocationJp;
	}

	public int getSrcIpLocationCn() {
		return srcIpLocationCn;
	}

	public void setSrcIpLocationCn(int srcIpLocationCn) {
		this.srcIpLocationCn = srcIpLocationCn;
	}

	public int getSrcIpLocationOther() {
		return srcIpLocationOther;
	}

	public void setSrcIpLocationOther(int srcIpLocationOther) {
		this.srcIpLocationOther = srcIpLocationOther;
	}

	public double[] getSrcPercentTh() {
		return srcPercentTh;
	}

	public void setSrcPercentTh(double[] srcPercentTh) {
		this.srcPercentTh = srcPercentTh;
	}

	public double getSrcAvgPercentTh() {
		return srcAvgPercentTh;
	}

	public void setSrcAvgPercentTh(double srcAvgPercentTh) {
		this.srcAvgPercentTh = srcAvgPercentTh;
	}

	public int getSrcThDomainCOM() {
		return srcThDomainCOM;
	}

	public void setSrcThDomainCOM(int srcThDomainCOM) {
		this.srcThDomainCOM = srcThDomainCOM;
	}

	public int getSrcThDomainTH() {
		return srcThDomainTH;
	}

	public void setSrcThDomainTH(int srcThDomainTH) {
		this.srcThDomainTH = srcThDomainTH;
	}

	public int getSrcThDomainNET() {
		return srcThDomainNET;
	}

	public void setSrcThDomainNET(int srcThDomainNET) {
		this.srcThDomainNET = srcThDomainNET;
	}

	public int getSrcThDomainORG() {
		return srcThDomainORG;
	}

	public void setSrcThDomainORG(int srcThDomainORG) {
		this.srcThDomainORG = srcThDomainORG;
	}

	public int getSrcThDomainINFO() {
		return srcThDomainINFO;
	}

	public void setSrcThDomainINFO(int srcThDomainINFO) {
		this.srcThDomainINFO = srcThDomainINFO;
	}

	public int getSrcThDomainOTHER() {
		return srcThDomainOTHER;
	}

	public void setSrcThDomainOTHER(int srcThDomainOTHER) {
		this.srcThDomainOTHER = srcThDomainOTHER;
	}

	public int getSrcNonDomainCOM() {
		return srcNonDomainCOM;
	}

	public void setSrcNonDomainCOM(int srcNonDomainCOM) {
		this.srcNonDomainCOM = srcNonDomainCOM;
	}

	public int getSrcNonDomainTH() {
		return srcNonDomainTH;
	}

	public void setSrcNonDomainTH(int srcNonDomainTH) {
		this.srcNonDomainTH = srcNonDomainTH;
	}

	public int getSrcNonDomainNET() {
		return srcNonDomainNET;
	}

	public void setSrcNonDomainNET(int srcNonDomainNET) {
		this.srcNonDomainNET = srcNonDomainNET;
	}

	public int getSrcNonDomainORG() {
		return srcNonDomainORG;
	}

	public void setSrcNonDomainORG(int srcNonDomainORG) {
		this.srcNonDomainORG = srcNonDomainORG;
	}

	public int getSrcNonDomainINFO() {
		return srcNonDomainINFO;
	}

	public void setSrcNonDomainINFO(int srcNonDomainINFO) {
		this.srcNonDomainINFO = srcNonDomainINFO;
	}

	public int getSrcNonDomainOTHER() {
		return srcNonDomainOTHER;
	}

	public void setSrcNonDomainOTHER(int srcNonDomainOTHER) {
		this.srcNonDomainOTHER = srcNonDomainOTHER;
	}

	public void setTrgInDegreeNonTh(int trgInDegreeNonTh) {
		this.trgInDegreeNonTh = trgInDegreeNonTh;
	}

	public int getTrgInDegreeNonTh() {
		return trgInDegreeNonTh;
	}

	public FeatureVector(String[] i) {
		super();

		this.hostname = i[0];
		// try{
		// this.hostname = URIUtil.decode(hostname).trim();
		// }catch(Exception e){}

		this.trgDomain = i[1];
		this.trgIpLocation = i[2];
		try {
			this.trgInDegreeTh = Integer.parseInt(i[3].trim());
		} catch (Exception e) {/* e.printStackTrace(); */
		}
		try {
			this.trgInDegreeNonTh = Integer.parseInt(i[4].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcIpLocationTh = Integer.parseInt(i[18].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcIpLocationUs = Integer.parseInt(i[19].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcIpLocationUk = Integer.parseInt(i[20].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcIpLocationJp = Integer.parseInt(i[21].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcIpLocationCn = Integer.parseInt(i[22].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcIpLocationOther = Integer.parseInt(i[23].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcPercentTh = new double[] {
					Double.parseDouble(i[24].trim()),
					Double.parseDouble(i[25]), Double.parseDouble(i[26]),
					Double.parseDouble(i[27]), Double.parseDouble(i[28]) };
		} catch (Exception e) { /* e.printStackTrace(); */

		}

		try {
			this.srcAvgPercentTh = Double.parseDouble(i[5].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcThDomainCOM = Integer.parseInt(i[6].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcThDomainTH = Integer.parseInt(i[7].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcThDomainNET = Integer.parseInt(i[8].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcThDomainORG = Integer.parseInt(i[9].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcThDomainINFO = Integer.parseInt(i[10].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcThDomainOTHER = Integer.parseInt(i[11].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcNonDomainCOM = Integer.parseInt(i[12].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcNonDomainTH = Integer.parseInt(i[13].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcNonDomainNET = Integer.parseInt(i[14].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcNonDomainORG = Integer.parseInt(i[15].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcNonDomainINFO = Integer.parseInt(i[16].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
		try {
			this.srcNonDomainOTHER = Integer.parseInt(i[17].trim());
		} catch (Exception e) {/* e.printStackTrace(); */

		}
	}

	public FeatureVector(String hostname, String trgDomain,
			String trgIpLocation, int trgInDegreeTh, int trgInDegreeNonTh,
			int srcIpLocationTh, int srcIpLocationUs, int srcIpLocationUk,
			int srcIpLocationJp, int srcIpLocationCn, int srcIpLocationOther,
			double[] srcPercentTh, double srcAvgPercentTh, int srcThDomainCOM,
			int srcThDomainTH, int srcThDomainNET, int srcThDomainORG,
			int srcThDomainINFO, int srcThDomainOTHER, int srcNonDomainCOM,
			int srcNonDomainTH, int srcNonDomainNET, int srcNonDomainORG,
			int srcNonDomainINFO, int srcNonDomainOTHER) {
		super();
		this.hostname = hostname;
		this.trgDomain = trgDomain;
		this.trgIpLocation = trgIpLocation;
		this.trgInDegreeTh = trgInDegreeTh;
		this.trgInDegreeNonTh = trgInDegreeNonTh;
		this.srcIpLocationTh = srcIpLocationTh;
		this.srcIpLocationUs = srcIpLocationUs;
		this.srcIpLocationUk = srcIpLocationUk;
		this.srcIpLocationJp = srcIpLocationJp;
		this.srcIpLocationCn = srcIpLocationCn;
		this.srcIpLocationOther = srcIpLocationOther;
		this.srcPercentTh = srcPercentTh;
		this.srcAvgPercentTh = srcAvgPercentTh;
		this.srcThDomainCOM = srcThDomainCOM;
		this.srcThDomainTH = srcThDomainTH;
		this.srcThDomainNET = srcThDomainNET;
		this.srcThDomainORG = srcThDomainORG;
		this.srcThDomainINFO = srcThDomainINFO;
		this.srcThDomainOTHER = srcThDomainOTHER;
		this.srcNonDomainCOM = srcNonDomainCOM;
		this.srcNonDomainTH = srcNonDomainTH;
		this.srcNonDomainNET = srcNonDomainNET;
		this.srcNonDomainORG = srcNonDomainORG;
		this.srcNonDomainINFO = srcNonDomainINFO;
		this.srcNonDomainOTHER = srcNonDomainOTHER;
	}

	public String[] toWekaFeatures() {
		String[] output = new String[28];
		output[0] = this.trgDomain;
		output[1] = this.trgIpLocation;
		output[2] = String.valueOf(this.trgInDegreeTh);
		output[3] = String.valueOf(this.trgInDegreeNonTh);
		output[4] = String.valueOf(this.srcAvgPercentTh);
		output[5] = String.valueOf(this.srcThDomainCOM);
		output[6] = String.valueOf(this.srcThDomainTH);
		output[7] = String.valueOf(this.srcThDomainNET);
		output[8] = String.valueOf(this.srcThDomainORG);
		output[9] = String.valueOf(this.srcThDomainINFO);
		output[10] = String.valueOf(this.srcThDomainOTHER);
		output[11] = String.valueOf(this.srcNonDomainCOM);
		output[12] = String.valueOf(this.srcNonDomainTH);
		output[13] = String.valueOf(this.srcNonDomainNET);
		output[14] = String.valueOf(this.srcNonDomainORG);
		output[15] = String.valueOf(this.srcNonDomainINFO);
		output[16] = String.valueOf(this.srcNonDomainOTHER);
		output[17] = String.valueOf(this.srcIpLocationTh);
		output[18] = String.valueOf(this.srcIpLocationUs);
		output[19] = String.valueOf(this.srcIpLocationUk);
		output[20] = String.valueOf(this.srcIpLocationJp);
		output[21] = String.valueOf(this.srcIpLocationCn);
		output[22] = String.valueOf(this.srcIpLocationOther);
		output[23] = String.valueOf(this.srcPercentTh[0]);
		output[24] = String.valueOf(this.srcPercentTh[1]);
		output[25] = String.valueOf(this.srcPercentTh[2]);
		output[26] = String.valueOf(this.srcPercentTh[3]);
		output[27] = String.valueOf(this.srcPercentTh[4]);
		return output;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getHostname() {
		return hostname;
	}

	public String toWeKaString() {
		String[] output = new String[28];
		output[0] = this.trgDomain;
		output[1] = this.trgIpLocation;
		output[2] = String.valueOf(this.trgInDegreeTh);
		output[3] = String.valueOf(this.trgInDegreeNonTh);
		output[4] = String.valueOf(this.srcAvgPercentTh);
		output[5] = String.valueOf(this.srcThDomainCOM);
		output[6] = String.valueOf(this.srcThDomainTH);
		output[7] = String.valueOf(this.srcThDomainNET);
		output[8] = String.valueOf(this.srcThDomainORG);
		output[9] = String.valueOf(this.srcThDomainINFO);
		output[10] = String.valueOf(this.srcThDomainOTHER);
		output[11] = String.valueOf(this.srcNonDomainCOM);
		output[12] = String.valueOf(this.srcNonDomainTH);
		output[13] = String.valueOf(this.srcNonDomainNET);
		output[14] = String.valueOf(this.srcNonDomainORG);
		output[15] = String.valueOf(this.srcNonDomainINFO);
		output[16] = String.valueOf(this.srcNonDomainOTHER);
		output[17] = String.valueOf(this.srcIpLocationTh);
		output[18] = String.valueOf(this.srcIpLocationUs);
		output[19] = String.valueOf(this.srcIpLocationUk);
		output[20] = String.valueOf(this.srcIpLocationJp);
		output[21] = String.valueOf(this.srcIpLocationCn);
		output[22] = String.valueOf(this.srcIpLocationOther);
		output[23] = String.valueOf(this.srcPercentTh[0]);
		output[24] = String.valueOf(this.srcPercentTh[1]);
		output[25] = String.valueOf(this.srcPercentTh[2]);
		output[26] = String.valueOf(this.srcPercentTh[3]);
		output[27] = String.valueOf(this.srcPercentTh[4]);

		String tmp = "";
		for (String t : output) {
			tmp += t + ",";
		}
		return tmp;
	}
}
