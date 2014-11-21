package at.ac.univie.isc.asio.metadata;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "resource_metadata_list")
public class RepositoryResponse {

  private List<DatasetMetadata> datasets = null;

  @XmlElement(name = "dataset")
  @XmlElementWrapper(name = "resource_metadata")
  public List<DatasetMetadata> getDatasets() {
    if (datasets == null) { datasets = new ArrayList<>(); }
    return datasets;
  }
}