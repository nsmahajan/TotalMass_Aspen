import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;

/****
 * Description : This class calculates the total mass of the elements from the
 * input json file. author: Neha Mahajan Version: 1 Date : 02/25/2017
 ****/

public class FindTotalMass {

	/*
	 * The HashMap stores the chemical elements and their molecular mass
	 * (grams/mol) using key-value pairs key{String} chemical element name
	 * value{Double} molecular mass in grams/mol
	 */
	private HashMap<String, Double> elementsMolecularMass = new HashMap<String, Double>();

	/*
	 * The HashMap stores the order of magnitude conversion i.e it converts
	 * units in words into there equivalent decimal representation for e.g. 1
	 * kilogram = 1000 grams BigDecimal is used to support the order of
	 * magnitudes like yotta/zetta/yocto/zepto key{String} units in words
	 * value{BigDecimal} decimal representation
	 */
	private HashMap<String, BigDecimal> unitConversion = new HashMap<String, BigDecimal>();

	public static void main(String[] args) {
		FindTotalMass ftm = new FindTotalMass();
		ftm.loadElementsData();
		ftm.loadUnitConversion();
		ftm.loadJSonObject();
	}

	/*
	 * The function reads the input file with json objects and calculates the
	 * total mass of elements.
	 * 
	 * @param none
	 * 
	 * @return void
	 */
	private void loadJSonObject() {
		System.out.println("Loading input file...\n");

		String inputFile = "./datasource/example1_json.txt";
		JsonReader reader = null;
		JsonObject elementObject = null;
		JsonObject object = null;
		String unit, elementName = "";

		BigDecimal totalmass = BigDecimal.ZERO;

		try {
			reader = Json.createReader(new FileReader(inputFile));
			elementObject = reader.readObject();

			// iterate through the array of elements to calculate the mass and
			// add it to the total mass
			JsonArray nodes = elementObject.getJsonArray("components");
			for (JsonValue jsonValue : nodes) {
				object = (JsonObject) jsonValue;
				unit = object.getString("units");
				elementName = object.getString("name").toLowerCase();

				// function call to validate the unit of mass
				if (isValidUnit(unit)) {

					// function call to validate the chemical element
					if (isValidElement(elementName)) {
						JsonNumber num = (JsonNumber) object.get("mass");

						// if the unit is mol convert it into grams
						if (unit.equals("mol")) {
							totalmass = totalmass
									.add(getGramsFromMols(Double.parseDouble(num.toString()), elementName));
						} else {
							// order of magnitude is converted into grams
							// equivalent
							totalmass = totalmass.add(
									new BigDecimal(num.toString()).multiply(unitConversion.get(unit.toLowerCase())));
						}
					} else {
						System.err.println("Element {" + elementName
								+ "} cannot not be found in element table.\nHence this entry will not be used in total mass calculation.");
					}
				} else {
					System.err.println("Unit: {" + unit + "}" + " for element " + elementName
							+ " is not valid.\nHence this entry will not be used in total mass calculation.");
				}
			}

			System.out.println("\n*********************OUTPUT**********************\n");
			System.out.print("Total mass: " + totalmass.setScale(2, RoundingMode.CEILING) + "grams or ");
			System.out.println(convertToLbs(totalmass) + "lbs");

		} catch (FileNotFoundException e) {
			System.err.println("Input File at path" + inputFile + " does not exists");
			System.err.println("Exiting the program");
		} catch (JsonParsingException jpse) {
			showErrorMessage(jpse);
		} catch (JsonException je) {
			showErrorMessage(je);
		} catch (NullPointerException npe) {
			showErrorMessage(npe);
		} catch (Exception e) {
			showErrorMessage(e);
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	/*
	 * Since all the exceptions where displaying same error messages, a single
	 * function is created to display the error message to keep the code clean.
	 * But we can have separate error messages displayed in the catch block for
	 * different types of exception. Function prints the error messages on the
	 * console.
	 * 
	 * @param none
	 * 
	 * @return void
	 */
	private void showErrorMessage(Exception e) {
		System.err.println("File could not be sucessfully parsed.Below are the details:");
		System.err.print(e);
		System.err.println("\tAt Line number: " + e.getStackTrace()[0].getLineNumber());
		System.err.println("Exiting the program");
	}

	/*
	 * The functions checks whether the element in the input json object is a
	 * valid chemical element or not
	 * 
	 * @param elementName name of the chemical element
	 * 
	 * @return boolean true if the element is a chemical element false if the
	 * element is not a chemical element
	 */
	private boolean isValidElement(String elementName) {
		return elementsMolecularMass.containsKey(elementName);
	}

	/*
	 * The functions converts the grams into lbs
	 * 
	 * @param totalmass total mass in grams
	 * 
	 * @return BigDecimal molecular mass in lbs (pound)
	 */
	private BigDecimal convertToLbs(BigDecimal totalmass) {
		return totalmass.divide(unitConversion.get("pound"), 2, RoundingMode.FLOOR);
	}

	/*
	 * The functions converts the mol unit into gram equivalent
	 * 
	 * @param noOfMol the number of mol of the element elementName name of the
	 * chemical element for which the conversion(mol to grams) is required.
	 * 
	 * @return BigDecimal molecular mass in grams
	 */
	private BigDecimal getGramsFromMols(double noOfMol, String elementName) {
		BigDecimal gramsPerMol = new BigDecimal(elementsMolecularMass.get(elementName));
		return gramsPerMol.multiply(new BigDecimal(noOfMol + ""));
	}

	/*
	 * The functions checks for the validation of a unit of mass
	 * 
	 * @param unit the unit of the mass for the element
	 * 
	 * @return boolean true - if the unit is a valid unit of mass false - if
	 * unit is invalid
	 */
	private boolean isValidUnit(String unit) {
		if (unitConversion.containsKey(unit) || unit.equals("mol"))
			return true;

		return false;
	}

	/*
	 * Read the csv to load the chemical elements and their molecular mass into
	 * the hashmap.
	 * 
	 * @param none
	 * 
	 * @return void
	 */
	private void loadElementsData() {
		System.out.println("Loading elements molecular mass data...\n");
		String elementMolMassFile = "./datasource/ElementsMolecularMass.csv";
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		String[] elementMassRecord;

		try {
			br = new BufferedReader(new FileReader(elementMolMassFile));
			while ((line = br.readLine()) != null) {
				elementMassRecord = line.split(cvsSplitBy);

				/*
				 * hashmap with key: chemical element value: molecular mass
				 * (grams/mol)
				 */
				elementsMolecularMass.put(elementMassRecord[1].toLowerCase(), Double.parseDouble(elementMassRecord[0]));
			}
		} catch (FileNotFoundException e) {
			System.err.println("File at path " + elementMolMassFile
					+ " not found. Please ensure file exists in src folder and run the progarm again.");
			System.err.println("Exiting the program\n");
		} catch (IOException e) {
			System.err.println("File at path " + elementMolMassFile
					+ " not found. Please ensure file exists in src folder and run the progarm again.");
			System.err.println("Exiting the program\n");
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		System.out.println("Sucessfully read elements molecular mass data into the program.\n");
	}

	/*
	 * Read the csv to load the unit in words and there decimal representation
	 * into the hashmap. Each unit is converted into grams equivalent.
	 * 
	 * @param none
	 * 
	 * @return void
	 */
	private void loadUnitConversion() {
		System.out.println("Loading unit conversion table...\n");

		String unitConversionFile = "./datasource/unitconversion.csv";
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		String[] unitConversionRow;

		try {
			br = new BufferedReader(new FileReader(unitConversionFile));
			while ((line = br.readLine()) != null) {
				unitConversionRow = line.split(cvsSplitBy);

				/*
				 * hashmap with key: units in words value: decimal
				 * representation
				 */
				unitConversion.put(unitConversionRow[0].toLowerCase(), new BigDecimal(unitConversionRow[1]));
			}
		} catch (FileNotFoundException e) {
			System.err.println("File at path " + unitConversionFile
					+ " not found. Please ensure file exists in src folder and run the progarm again.");
			System.err.println("Exiting the program\n");
		} catch (IOException e) {
			System.err.println("File at path " + unitConversionFile
					+ " not found. Please ensure file exists in src folder and run the progarm again.");
			System.err.println("Exiting the program\n");
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		System.out.println("Sucessfully read unit conversion table into the program.\n");
	}

}
