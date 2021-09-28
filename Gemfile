# frozen_string_literal: true

SHARED_GEMFILE = './datalayer/Gemfile'
eval_gemfile(SHARED_GEMFILE) if File.exists?(SHARED_GEMFILE)

source "https://rubygems.org"

git_source(:github) { |repo_name| "https://github.com/#{repo_name}" }

ruby '2.7.2'

gem 'capybara', '~> 3.35'
gem 'cider_ci-open_session', '>= 1.0.0', '< 2.0.0'
gem 'database_cleaner-active_record'
gem 'faraday', '~> 1.7'
gem 'faraday_middleware'
gem 'geckodriver-helper', '~> 0.24'
gem 'rspec'
gem 'selenium-webdriver', '~> 3.142.0'
