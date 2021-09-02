require 'capybara/rspec'
ENV["RAILS_ENV"] = "test"
require './datalayer/config/environment'
require 'config/http_client'
require 'config/helpers'

def base_url
  "http://localhost:3180"
end

RSpec.configure do |config|
  raise "Run tests in test environment: `RAILS_ENV=test rspec spec/`" unless Rails.env.test?

  config.expect_with :rspec do |expectations|
    expectations.include_chain_clauses_in_custom_matcher_descriptions = true
  end

  config.mock_with :rspec do |mocks|
    mocks.verify_partial_doubles = true
  end

  config.shared_context_metadata_behavior = :apply_to_host_groups

  config.include Config::HTTPClient
  config.include Config::Helpers
  config.include FactoryGirl::Syntax::Methods

  config.before(:suite) do
    FactoryGirl.definition_file_paths = %w{./spec/factories}
    FactoryGirl.find_definitions
    DatabaseCleaner.strategy = :truncation
    DatabaseCleaner.clean
  end

  config.around(:each) do |example|
    DatabaseCleaner.cleaning do
      example.run
    end
  end

  Capybara.default_driver = :selenium
  Capybara.app_host = base_url
end
