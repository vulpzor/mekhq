/*
 * KFDriveCoil.java
 * 
 * Copyright (c) 2019, The MegaMek Team
 * 
 * This file is part of MekHQ.
 * 
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.parts;

import java.io.PrintWriter;

import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Jumpship;
import megamek.common.LandAirMech;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author MKerensky
 */
public class KFDriveCoil extends Part {

	/**
     * 
     */
    private static final long serialVersionUID = 4515211961051281110L;
    
    static final TechAdvancement TA_STANDARD_CORE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2107, 2120, 2300).setPrototypeFactions(F_TA)
            .setProductionFactions(F_TA).setTechRating(RATING_D)
            .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    static final TechAdvancement TA_NO_BOOM = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2304, 2350, 2364, 2520).setPrototypeFactions(F_TA)
            .setProductionFactions(F_TH).setTechRating(RATING_B)
            .setAvailability(RATING_C, RATING_X, RATING_X, RATING_X)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    
    //Standard, primitive, compact, subcompact...
    private int coreType;
    
    public int getCoreType() {
        return coreType;
    }

    public KFDriveCoil() {
    	this(0, Jumpship.DRIVE_CORE_STANDARD, null);
    }
    
    public KFDriveCoil(int tonnage, int coreType, Campaign c) {
        super(tonnage, c);
        this.coreType = coreType;
        this.name = "K-F Drive Coil";
    }
        
    public KFDriveCoil clone() {
    	KFDriveCoil clone = new KFDriveCoil(0, coreType, campaign);
        clone.copyBaseData(this);
    	return clone;
    }
    
	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		int priorHits = hits;
		if(null != unit) {
		    if (unit.getEntity() instanceof Jumpship) {
    			if(((Jumpship)unit.getEntity()).getKFDriveCoilHit()) {
    				hits = 1;
    			} else {
    				hits = 0;
    			}
		    }
			if(checkForDestruction 
					&& hits > priorHits 
					&& Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
				remove(false);
			}
		}
	}
	
	@Override 
	public int getBaseTime() {
	    int time;
		if(isSalvaging()) {
		    //SO KF Drive times, p184-5
			time = 28800;
		} else {
		    time = 4800;
		}
		return time;
	}
	
	@Override
	public int getDifficulty() {
	    //SO Difficulty Mods
		if(isSalvaging()) {
			return 2;
		}
		return 5;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Jumpship) {
		        ((Jumpship)unit.getEntity()).setKFDriveCoilHit(needsFixing());
		}
	}

	@Override
	public void fix() {
		super.fix();
		if (null != unit && unit.getEntity() instanceof Jumpship) {
		    Jumpship js = ((Jumpship)unit.getEntity());
			js.setKFDriveCoilHit(false);
			//Also repair your KF Drive integrity - +1 point if you have other components to fix
			//Otherwise, fix it all.
			if (js.isKFDriveDamaged()) {
			    js.setKFIntegrity(Math.min((js.getKFIntegrity() + 1), js.getOKFIntegrity()));
			} else {
			    js.setKFIntegrity(js.getOKFIntegrity());
			}
		}
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
		    if (unit.getEntity() instanceof Jumpship) {
		        ((Jumpship)unit.getEntity()).setKFDriveCoilHit(true);
		    }
	        //All the BT lore says you can't jump while carrying around another KF Drive, therefore
			//you can't salvage and keep this in the warehouse, just remove/scrap and replace it
		    //See SO p130 for reference
			campaign.removePart(this);
			unit.removePart(this);
			Part missing = getMissingPart();
			unit.addPart(missing);
			campaign.addPart(missing, 0);
		}
		setUnit(null);
		updateConditionFromEntity(false);
	}

	@Override
	public MissingPart getMissingPart() {
		return new MissingKFDriveCoil(getUnitTonnage(), campaign);
	}

	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}

	@Override
	public Money getStickerPrice() {
		return Money.of(10.0 * getUnitTonnage());
	}

	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public int getTechRating() {
		//go with conventional fighter avionics
		return EquipmentType.RATING_B;
	}

	@Override
	public boolean isSamePartType(Part part) {
		return part instanceof KFDriveCoil;
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<coreType>"
                +coreType
                +"</coreType>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();
        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            
            if (wn2.getNodeName().equalsIgnoreCase("coreType")) {
                coreType = Integer.parseInt(wn2.getTextContent());
            } 
        }
	}
	
	@Override
	public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_VESSEL);
	}

    @Override
    public String getLocationName() {
        return null;
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }
    
	@Override
	public TechAdvancement getTechAdvancement() {
	    return TA_GENERIC;
	}
	
}
