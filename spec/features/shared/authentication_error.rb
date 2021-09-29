RSpec.configure do |c|
  c.alias_it_should_behave_like_to :it_displays
end

shared_examples "authentication error" do
  it "displays 'not signed in' error" do
    visit path
    expect(page).to have_content("You are not signed in!")
  end
end
