package com.ahb.enq.rtns;

/**
 * TODO: Document me!
 *
 * @author 01965
 *
 */
import java.util.ArrayList;
import java.util.List;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;

/**
 * TODO: Document me!
 *
 * @author 01965
 *
 */
public class AhbEClientChgPds extends Enquiry {

    @Override
    public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext){

        String feeType        = "";
        String AcctId         = "";
        String Target         = "";
        String CustomerId     = "";
        String selId          = "";
        // Get number of selection fields
        int size = filterCriteria.size();                       
        for(int i= 0 ; i<size; i++){
            if(filterCriteria.get(i).getFieldname().equals("FeeProduct")){
                feeType = filterCriteria.get(i).getValue().toString();   //get charge type
            }else if(filterCriteria.get(i).getFieldname().equals("CustomerAccountNumber")){
                AcctId = filterCriteria.get(i).getValue().toString(); //get account number
            }
        }

        if(feeType.isEmpty() && AcctId.isEmpty()){
            throw new T24CoreException("Both feeType and AcctId are mandatory","EB-ERROR.SELECTION");
        }else{             
            //Read account and get Customer ID
            try{
                DataAccess dataAccess = new DataAccess(this);
                AccountRecord accountRecord = new AccountRecord(dataAccess.getRecord("ACCOUNT", AcctId));
                CustomerId = accountRecord.getCustomer().getValue().toString();
                if(!CustomerId.isEmpty()){
                    //Read customer - get Target 
                    CustomerRecord customerRecord = new CustomerRecord(dataAccess.getRecord("CUSTOMER", CustomerId));
                    Target = customerRecord.getTarget().getValue().toString();
                    //Get the selection ID         
                    selId = setselId(feeType, AcctId, CustomerId, Target);
                    if(selId.isEmpty()){
                        throw new T24CoreException(feeType + AcctId + "selId" + selId + "cif" + CustomerId + "tt" + Target , "EB-ERROR.SELECTION");
                    }
                }
            }catch(Exception e) {
                String selrecs = "INTL" + feeType + "POA" +"*" +"*" +"*" +"*" +"*";
                selId = selPds(selrecs);
            }
            //Update the new selection ID
            if(!selId.isEmpty()){
                FilterCriteria fc = new FilterCriteria();
                fc.setFieldname("@ID");
                fc.setOperand("EQ");
                fc.setValue(selId);
                filterCriteria.clear(); //clear the existing selection
                filterCriteria.add(fc);  //add newly formed selection 
            }
        }
        return filterCriteria;
    }

    private String setselId(String feeType, String AcctId, String CustomerId, String Target){
        String selRecord = "";
        //Check for all possible values
        for(int k = 0 ; k < 4; k++){
            String selvalue  = ""; String selrec = "";
            selrec    = getSelCon(k,feeType,AcctId,CustomerId,Target); 
            selvalue = selPds(selrec);
            if(!selvalue.isEmpty()){
                selRecord = selvalue;
                break;
            }
        }     
        return selRecord;
    }

    private String getSelCon(int j, String fType, String AcId, String CuId, String Targ){
        Session ss = new Session(this);
        String lcy = ss.getLocalCurrency().toString();
        String selrec = "";
        switch(j){
        case 0:
            selrec = "INTL" + fType + "POA" + Targ + CuId + AcId + lcy +"*";
            break;
        case 1:
            selrec = "INTL" + fType + "POA" + Targ + CuId +"*" +"*" +"*";
            break;
        case 2:    
            selrec = "INTL" + fType + "POA" + Targ +"*" +"*" +"*" +"*";
            break;
        case 3:    
            selrec = "INTL" + fType + "POA" +"*" +"*" +"*" +"*" +"*";
            break;
        default:   
            selrec = "INTL" + fType + "POA" +"*" +"*" +"*" +"*" +"*";
            break;
        }
        return selrec;
    }

    private String selPds(String selcon){
        List<String> id  = new ArrayList<String>(); 
        DataAccess da = new DataAccess(this);
        String selRec = ""; int sz = 0; 
        String selCmd = "WITH @ID LIKE " + selcon + "...";
        id       = da.selectRecords("", "PP.CLIENTCHARGES.PDS", "", selCmd);
        sz       = id.size();
        if (sz > 0 ){
            //Get the last element
            selRec = id.get(sz-1);
        }
        return selRec;
    }
}