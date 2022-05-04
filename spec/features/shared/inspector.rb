
shared_context :inspector_config do
  let(:inspector_config) do
    YAML.load_file(
      PROJECT_DIR.join("inspector-config.yml")
    ).with_indifferent_access
  end
end

shared_context :inspector do
  include_context :inspector_config
  let(:inspector) do
    FactoryGirl.create :inspector,
      id: inspector_config[:id],
      enabled: true,
      public_key: inspector_config[:'key-pair'][:'public-key']
  end
  before :each do
    @inspector = inspector
  end
end
