package au.edu.ardc.registry.oai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ListMetadataFormatFragment {
    @JsonProperty("ListMetadataFormats")
    private ListMetadataFormatFragment listMetadataFormat;
    private List<MetadataFormatFragment> metadataFormatFragments;

    public ListMetadataFormatFragment() {
        this.listMetadataFormat = listMetadataFormat;
    }

    public ListMetadataFormatFragment getListMetadataFormat() {
        return listMetadataFormat;
    }

    public void setListMetadataFormat(ListMetadataFormatFragment listMetadataFormat) {
        this.listMetadataFormat = listMetadataFormat;
    }
    public void appendMetadataFormatFragments(MetadataFormatFragment newFragment ){
        metadataFormatFragments.add(newFragment);
    }
}
