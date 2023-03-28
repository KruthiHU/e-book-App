package com.example.ecloudapp.filters;

import android.widget.Filter;

import com.example.ecloudapp.adapters.AdapterCategory;
import com.example.ecloudapp.models.ModelCategory;

import java.util.ArrayList;

public class FilterCategory extends Filter {
    //arraylist in which we want to search
    final ArrayList<ModelCategory> filterList;
    //adapter in which filter need to be implemented
    final AdapterCategory adapterCategory;

    //constructor
    public FilterCategory(ArrayList<ModelCategory> filterList, AdapterCategory adapterCategory) {
        this.filterList = filterList;
        this.adapterCategory = adapterCategory;
    }


    @Override
    protected FilterResults performFiltering(CharSequence charSequence) {
        FilterResults filterresults = new FilterResults();
       // value should not be null and empty
        if(charSequence != null && charSequence.length() > 0){
      // change to upper case, or lower case to avoid case sensitivity
        charSequence = charSequence.toString().toUpperCase();
        ArrayList<ModelCategory> filteredModels = new ArrayList<>();
        for(int i=0; i< filterList.size(); i++){
            // validate
            if (filterList.get(i).getCategory().toUpperCase().contains(charSequence)){
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
        return filterresults;

    }

    @Override
    protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
         //apply filter changes
        adapterCategory.categoryArrayList = (ArrayList<ModelCategory>)filterResults.values;

        // notify changes
        adapterCategory.notifyDataSetChanged();
    }
}
