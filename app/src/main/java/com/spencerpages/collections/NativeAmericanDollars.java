/*
 * Coin Collection, an Android app that helps users track the coins that they've collected
 * Copyright (C) 2010-2016 Andrew Williams
 *
 * This file is part of Coin Collection.
 *
 * Coin Collection is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Coin Collection is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Coin Collection.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.spencerpages.collections;

import android.database.sqlite.SQLiteDatabase;

import com.coincollection.CoinPageCreator;
import com.coincollection.CoinSlot;
import com.coincollection.CollectionInfo;
import com.coincollection.DatabaseHelper;
import com.spencerpages.MainApplication;
import com.spencerpages.R;

import java.util.ArrayList;
import java.util.HashMap;

public class NativeAmericanDollars extends CollectionInfo {

    private static final String COLLECTION_TYPE = "Sacagawea/Native American Dollars"; // Was: Sacagawea Dollars

    private static final Object[][] NATIVE_IMAGE_IDENTIFIERS = {
            {"2009", R.drawable.native_2009_unc,       R.drawable.native_2009_unc_25},
            {"2010", R.drawable.native_2010_unc,       R.drawable.native_2010_unc_25},
            {"2011", R.drawable.native_2011_unc,       R.drawable.native_2011_unc_25},
            {"2012", R.drawable.native_2012_unc,       R.drawable.native_2012_unc_25},
            {"2013", R.drawable.native_2013_proof,     R.drawable.native_2013_proof_25},
            {"2014", R.drawable.native_2014_unc,       R.drawable.native_2014_unc_25},
            {"2015", R.drawable.native_2015_unc,       R.drawable.native_2015_unc_25},
            {"2016", R.drawable.native_2016_unc,       R.drawable.native_2016_unc_25},
            {"2017", R.drawable.native_2017_unc,       R.drawable.native_2017_unc_25},
            {"2018", R.drawable.native_2018_unc,       R.drawable.native_2018_unc_25},
            {"2019", R.drawable.native_2019_unc,       R.drawable.native_2019_unc_25},
            {"2020", R.drawable.native_2020_unc,       R.drawable.native_2020_unc_25},
    };

    private static final HashMap<String, Integer[]> NATIVE_INFO = new HashMap<>();

    static {
        // Populate the NATIVE_INFO HashMap for quick image ID lookups later
        for (Object[] coinData : NATIVE_IMAGE_IDENTIFIERS){
            NATIVE_INFO.put((String) coinData[0],
                    new Integer[]{(Integer) coinData[1], (Integer) coinData[2]});
        }
    }

    private static final Integer START_YEAR = 2000;
    private static final Integer STOP_YEAR = CoinPageCreator.OPTVAL_STILL_IN_PRODUCTION;

    private static final int OBVERSE_IMAGE_COLLECTED = R.drawable.obv_sacagawea_unc;
    private static final int OBVERSE_IMAGE_MISSING = R.drawable.obv_sacagawea_unc_25;

    private static final int REVERSE_IMAGE = R.drawable.rev_sacagawea_unc;

    public String getCoinType() { return COLLECTION_TYPE; }

    public int getCoinImageIdentifier() { return REVERSE_IMAGE; }

    public int getCoinSlotImage(CoinSlot coinSlot){
        Integer[] slotImages = NATIVE_INFO.get(coinSlot.getIdentifier());
        boolean inCollection = coinSlot.isInCollection();
        if(slotImages != null){
            return slotImages[inCollection ? 0 : 1];
        } else {
            return inCollection ? OBVERSE_IMAGE_COLLECTED : OBVERSE_IMAGE_MISSING;
        }
    }

    public void getCreationParameters(HashMap<String, Object> parameters) {

        parameters.put(CoinPageCreator.OPT_EDIT_DATE_RANGE, Boolean.FALSE);
        parameters.put(CoinPageCreator.OPT_START_YEAR, START_YEAR);
        parameters.put(CoinPageCreator.OPT_STOP_YEAR, STOP_YEAR);
        parameters.put(CoinPageCreator.OPT_SHOW_MINT_MARKS, Boolean.FALSE);

        // Use the MINT_MARK_1 checkbox for whether to include 'P' coins
        parameters.put(CoinPageCreator.OPT_SHOW_MINT_MARK_1, Boolean.TRUE);
        parameters.put(CoinPageCreator.OPT_SHOW_MINT_MARK_1_STRING_ID, R.string.include_p);

        // Use the MINT_MARK_2 checkbox for whether to include 'D' coins
        parameters.put(CoinPageCreator.OPT_SHOW_MINT_MARK_2, Boolean.FALSE);
        parameters.put(CoinPageCreator.OPT_SHOW_MINT_MARK_2_STRING_ID, R.string.include_d);
    }

    // TODO Perform validation and throw exception
    public void populateCollectionLists(HashMap<String, Object> parameters, ArrayList<CoinSlot> coinList) {

        Integer startYear       = (Integer) parameters.get(CoinPageCreator.OPT_START_YEAR);
        Integer stopYear        = (Integer) parameters.get(CoinPageCreator.OPT_STOP_YEAR);
        Boolean showMintMarks   = (Boolean) parameters.get(CoinPageCreator.OPT_SHOW_MINT_MARKS);
        Boolean showP           = (Boolean) parameters.get(CoinPageCreator.OPT_SHOW_MINT_MARK_1);
        Boolean showD           = (Boolean) parameters.get(CoinPageCreator.OPT_SHOW_MINT_MARK_2);

        for(int i = startYear; i <= stopYear; i++){

            if(showMintMarks){
                if(showP){
                    coinList.add(new CoinSlot(Integer.toString(i), "P"));
                }
            } else {
                coinList.add(new CoinSlot(Integer.toString(i), ""));
            }

            if(showMintMarks && showD){
                coinList.add(new CoinSlot(Integer.toString(i), "D"));
            }
        }
    }
    public String getAttributionString(){
        return MainApplication.DEFAULT_ATTRIBUTION;
    }

    public int onCollectionDatabaseUpgrade(SQLiteDatabase db, String tableName,
                                           int oldVersion, int newVersion) {

        int total = 0;

        if(oldVersion <= 3) {
            // Add in new 2013 coins if applicable
            int value = DatabaseHelper.addFromYear(db, tableName, "2013");
            total += value;
        }

        if (oldVersion <= 4) {
            // Add in new 2014 coins if applicable
            int value = DatabaseHelper.addFromYear(db, tableName, "2014");
            total += value;
        }

        if (oldVersion <= 6) {
            // Add in new 2015 coins if applicable
            int value = DatabaseHelper.addFromYear(db, tableName, "2015");
            total += value;
        }

        if (oldVersion <= 7) {
            // Add in new 2016 coins if applicable
            int value = DatabaseHelper.addFromYear(db, tableName, "2016");
            total += value;
        }

        if (oldVersion <= 8) {
            // Add in new 2017 coins if applicable
            int value = DatabaseHelper.addFromYear(db, tableName, "2017");
            total += value;
        }

        if (oldVersion <= 11) {
            // Add in new 2018 coins if applicable
            int value = DatabaseHelper.addFromYear(db, tableName, "2018");
            total += value;
        }

        if (oldVersion <= 12) {
            // Add in new 2019 coins if applicable
            int value = DatabaseHelper.addFromYear(db, tableName, "2019");
            total += value;
        }

        if (oldVersion <= 13) {
            // Add in new 2020 coins if applicable
            int value = DatabaseHelper.addFromYear(db, tableName, "2020");
            total += value;
        }

        return total;
    }
}
