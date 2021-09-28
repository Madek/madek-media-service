require 'spec_helper'
require 'pry'

describe "displaying dashboard", type: :feature do
  before do
    visit "/"
    # TODO there is something missing here
    user = User.find_by(login: 'adam')
    expect(user).to be
    cookie_value = MadekOpenSession.build_session_value(user)
    page.driver.browser.manage.add_cookie(name: "madek-session", value: cookie_value)
  end

  it "displays it ;)" do
    visit '/media-service/'

    expect(page).to have_css('.navbar', text: 'Adam Admin')
  end
end
