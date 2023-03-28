package com.example.ecloudapp.filters;

import android.widget.Filter;


import com.example.ecloudapp.adapters.AdapterPdfAdmin;
import com.example.ecloudapp.models.ModelPdf;

import java.util.ArrayList;

public class FilterPdfAdmin extends Filter {
    //arraylist in which we want to search
    final ArrayList<ModelPdf> filterList;
    //adapter in which filter need to be implemented
    final AdapterPdfAdmin adapterPdfAdmin;

    //constructor
    public FilterPdfAdmin(ArrayList<ModelPdf> filterList, AdapterPdfAdmin adapterPdfAdmin) {
        this.filterList = filterList;
        this.adapterPdfAdmin = adapterPdfAdmin;
    }


    @Override
    protected FilterResults performFiltering(CharSequence charSequence) {
        FilterResults filterresults = new FilterResults();
       // value should not be null and empty
        if(charSequence != null && charSequence.length() > 0){
      // change to upper case, or lower case to avoid case sensitivity
        charSequence = charSequence.toString().toUpperCase();
        ArrayList<ModelPdf> filteredModels = new ArrayList<>();
        for(int i=0; i< filterList.size(); i++){
            // validate
            if (filterList.get(i).getTitle().toUpperCase().contains(charSequence)){
            // add to filtered list
                filteredModels.add(filterList.get(i));
            }
        }
            filterresults.count = filteredModels.size();
            filterresults.values = filteredModels;
        }
        else {
            filterresults.count = filterList.size();
            filterresults.values = filterList;
        }
        return filterresults; // dont miss it

    }

    @Override
    protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
         //apply filter changes
        adapterPdfAdmin.pdfArrayList = (ArrayList<ModelPdf>)filterResults.values;

        // notify changes
        adapterPdfAdmin.notifyDataSetChanged();
    }
}
